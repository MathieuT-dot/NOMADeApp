package com.nomade.android.nomadeapp.communication;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import com.nomade.android.nomadeapp.helperClasses.MyLog;
import com.nomade.android.nomadeapp.helperClasses.Utilities;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * FT311UARTInterface
 *
 * Class to handle the communication over USB with an FT311D or FT312D chip from FTDI Chip
 */
public class FT311UARTInterface {

    private static final String TAG = "FT311UARTInterface";

    private static final String ACTION_USB_PERMISSION =    "com.UARTLoopback.USB_PERMISSION";
    private UsbManager usbmanager;
    private PendingIntent mPermissionIntent;
    private ParcelFileDescriptor filedescriptor = null;
    private FileInputStream inputstream;
    private FileOutputStream outputstream;
    private boolean mPermissionRequestPending = false;

    private byte [] usbdata;
    private byte []	writeusbdata;
    private byte [] readBuffer; /*circular buffer*/
    private int totalBytes;
    private int writeIndex;
    private int readIndex;
    private byte status;
    private final int  maxnumbytes = 65535;

    private boolean read_enable = false;

    private Context global_context;

    public int length = 0;

    private boolean disconnected = false;

    public FT311UARTInterface(Context context) {
        super();
        global_context = context;
        usbdata = new byte[4096];
        writeusbdata = new byte[4096];
        readBuffer = new byte [maxnumbytes];

        readIndex = 0;
        writeIndex = 0;

        usbmanager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        context.registerReceiver(mUsbReceiver, filter);

        inputstream = null;
        outputstream = null;
    }

    /**
     * Sets and sends the default configuration for the USB chip.
     */
    public void SetDefaultConfig() {
        // default baud: 921600
        int baud = 921600;

        // default data bits: 8
        byte dataBits = 8;

        // default stop bits: 1
        byte stopBits = 1;

        // default parity: 0 (none)
        byte parity = 0;

        // default flow control: 1 (CTS/RTS)
        byte flowControl = 1;

        MyLog.d(TAG, "SetDefaultConfig | Baud rate: " + baud + ", Data bits: " + dataBits + ", Stop bits: " + stopBits + ", Parity: " + parity + ", Flow control: " + flowControl);

        /*prepare the baud rate buffer*/
        writeusbdata[0] = (byte)baud;
        writeusbdata[1] = (byte)(baud >> 8);
        writeusbdata[2] = (byte)(baud >> 16);
        writeusbdata[3] = (byte)(baud >> 24);

        /*data bits*/
        writeusbdata[4] = dataBits;

        /*stop bits*/
        writeusbdata[5] = stopBits;

        /*parity*/
        writeusbdata[6] = parity;

        /*flow control*/
        writeusbdata[7] = flowControl;

        /*send the UART configuration packet*/
        SendPacket(8);
    }

    /**
     * Writes the data to the usb write buffer.
     *
     * @param numBytes number of bytes to write
     * @param buffer data
     * @return status of the write operation
     */
    public byte SendData(int numBytes, byte[] buffer) {
        status = 0x00; /*success by default*/
        /*
         * if num bytes are more than maximum limit
         */
        if(numBytes < 1){
            /*return the status with the error in the command*/
            return status;
        }

        /*prepare the packet to be sent*/
        System.arraycopy(buffer, 0, writeusbdata, 0, numBytes);

        SendPacket(numBytes);

        return status;
    }

    /**
     * Reads data from the usb read buffer and checks and creates the telegrams.
     *
     * @param usbTelegramList list to add the correct telegrams to
     * @return status of the read operation
     */
    public byte readUsbTelegrams(List<UsbTelegram> usbTelegramList) {
        status = 0x00; /*success by default*/

        if (disconnected){
            status = (byte) 0xFF;
            return status;
        }

        /*should be at least one byte to read*/
        if(totalBytes == 0){
            status = 0x01;
            return status;
        }

        boolean dataAvailable = true;

        byte sd;
        byte da;
        byte sa;
        byte fc;
        byte dsap;
        byte ssap;
        byte[] du;
        byte fcsRead;
        byte fcsCalc;
        byte ed;

        StringBuilder stringBuilderSds = new StringBuilder();

        while (dataAvailable) {

            /*should be at least one byte to read*/
            if(totalBytes == 0){
                status = 0x01;
                return status;
            }

            byte[] data = new byte[1024];
            int telegramIndex = readIndex;

            int dataLength = totalBytes;

            if (dataLength > 1024) {
                dataLength = 1024;
            }

            /*copy to the user buffer*/
            for(int count = 0; count < dataLength; count++)
            {
                data[count] = readBuffer[telegramIndex];
                telegramIndex++;
                /*shouldnt read more than what is there in the buffer,
                 * 	so no need to check the overflow
                 */
                telegramIndex %= maxnumbytes;
            }

//            byte[] debugBytes = new byte[77];
//            for (int i = 0; i < 77; i++) {
//                debugBytes[i] = data[i];
//            }
//            MyLog.d(TAG, "readUsbTelegrams: first 77 bytes read = " + Utilities.bytesToHex(debugBytes));

            sd = 0x00;
            da = 0x00;
            sa = 0x00;
            fc = 0x00;
            dsap = 0x00;
            ssap = 0x00;
            du = null;
            fcsRead = 0x00;
            fcsCalc = 0x00;
            ed = 0x00;

            sd = data[0];

            if (sd == UsbTelegram.SD2) {

                if (!stringBuilderSds.toString().equals("")) {
                    MyLog.e(TAG, "readUsbTelegrams: Wrong start delimiters = " + stringBuilderSds.toString());
                }

                if (dataLength >= 4){

                    byte le = data[1];
                    byte ler = data[2];

                    if (le == ler){

                        if (dataLength >= (le & 0xFF) + 6){

                            byte  sd2 = data[3];

                            if (sd2 == UsbTelegram.SD2){

                                ed = data[(le & 0xFF) + 5];

                                if (ed == UsbTelegram.ED){

                                    for (int i = 0; i < (le & 0xFF); i++){
                                        fcsCalc += data[i + 4];
                                    }
                                    fcsRead = data[(le & 0xFF) + 4];

                                    if (fcsCalc  == fcsRead){

                                        da = data[4];
                                        sa = data[5];
                                        fc = data[6];

                                        if ((da & 0x80) == 0x80 && (sa & 0x80) == 0x80){
                                            dsap = data[7];
                                            ssap = data[8];
                                            du = Arrays.copyOfRange(data, 9, (le & 0xFF) + 4);

                                            usbTelegramList.add(new UsbTelegram(sd, da, sa, fc, dsap, ssap, du));

                                            readIndex += (le & 0xFF) + 6;
                                            readIndex %= maxnumbytes;
                                            totalBytes -= (le & 0xFF) + 6;
                                        }
                                        else {
                                            du = Arrays.copyOfRange(data, 7, (le & 0xFF) + 4);

                                            usbTelegramList.add(new UsbTelegram(sd, da, sa, fc, dsap, ssap, du));

                                            readIndex += (le & 0xFF) + 6;
                                            readIndex %= maxnumbytes;
                                            totalBytes -= (le & 0xFF) + 6;
                                        }
                                    }
                                    else {
                                        MyLog.e(TAG, "Frame check sequence wrong (read: " + fcsRead + " | calculated: " + fcsCalc + " | dataLength: " + dataLength +  " | totalBytes: " + totalBytes + ")");
//                                        MyLog.d(TAG, "readUsbTelegrams: first 77 bytes read = " + Utilities.bytesToHex(debugBytes));

                                        readIndex++;
                                        readIndex %= maxnumbytes;
                                        totalBytes--;
                                    }
                                }
                                else {
                                    MyLog.e(TAG, "Missing or wrong end delimiter (ed: " + Utilities.byteToHex(ed) + " | dataLength: " + dataLength + " | totalBytes: " + totalBytes + ")");
//                                    MyLog.d(TAG, "readUsbTelegrams: first 77 bytes read = " + Utilities.bytesToHex(debugBytes));

                                    readIndex++;
                                    readIndex %= maxnumbytes;
                                    totalBytes--;
                                }
                            }
                            else {
                                MyLog.e(TAG, "Missing start delimiter 2 (sd: " + Utilities.byteToHex(sd2) + " | dataLength: " + dataLength + " | totalBytes: " + totalBytes + ")");
//                                MyLog.d(TAG, "readUsbTelegrams: first 77 bytes read = " + Utilities.bytesToHex(debugBytes));

                                readIndex++;
                                readIndex %= maxnumbytes;
                                totalBytes--;
                            }
                        }
                        else {
//                            MyLog.w(TAG, "Total bytes in data buffer is shorter than the SD2 telegram length (Bytes in data buffer: " + dataLength + " | SD2 length: " + (le & 0xFF) + "+6 | Total bytes: " + totalBytes + ")");
                            dataAvailable = false;
                        }
                    }
                    else {
                        MyLog.e(TAG, "Read length and length repeated doesn't match (le: " + (le & 0xFF) + " | ler: " + (ler & 0xFF) + " | dataLength: " + dataLength + " | totalBytes: " + totalBytes + ")");
//                        MyLog.d(TAG, "readUsbTelegrams: first 77 bytes read = " + Utilities.bytesToHex(debugBytes));

                        readIndex++;
                        readIndex %= maxnumbytes;
                        totalBytes--;
                    }
                }
                else {
//                    MyLog.w(TAG, "Total bytes in data buffer is shorter than the minimum SD2 telegram length (Bytes in data buffer: " + dataLength + " | SD2 minimum length: 9 | Total bytes: " + totalBytes + ")");
                    dataAvailable = false;
                }

            }
            else {

                readIndex++;
                readIndex %= maxnumbytes;
                totalBytes--;

                stringBuilderSds.append(Utilities.byteToHex(sd));
                stringBuilderSds.append(" ");

            }

//            switch (sd){
//
//                case UsbTelegram.SD1:
//
//                    if (totalBytes >= 6){
//                        da = data[1];
//                        sa = data[2];
//                        fc = data[3];
//                        fcsRead = data[4];
//                        ed = data[5];
//
//                        if (ed == UsbTelegram.ED){
//
//                            fcsCalc = (byte) (da + sa + fc);
//
//                            if (fcsCalc == fcsRead){
//                                usbTelegramList.add(new UsbTelegram(sd, da, sa, fc, dsap, ssap, null));
//
//                                readIndex += 6;
//                                readIndex %= maxnumbytes;
//                                totalBytes -= 6;
//                            }
//                            else {
//                                MyLog.e(TAG, "Frame check sequence wrong (read: " + fcsRead + ", calculated: " + fcsCalc + ")");
//
//                                readIndex++;
//                                readIndex %= maxnumbytes;
//                                totalBytes--;
//                            }
//                        }
//                        else {
//                            MyLog.e(TAG, "Missing or wrong end delimiter (ed: " + ed + ")");
//
//                            readIndex++;
//                            readIndex %= maxnumbytes;
//                            totalBytes--;
//                        }
//                    }
//                    else {
////                        MyLog.w(TAG, "Total bytes in buffer is shorter than the SD1 telegram length (Total bytes: " + totalBytes + " | SD1 length: 6)");
//                        dataAvailable = false;
//                    }
//
//                    break;
//
//                case UsbTelegram.SD2:
//
//                    if (!stringBuilderSds.toString().equals("")) {
//                        MyLog.e(TAG, "readUsbTelegrams: Wrong start delimiters = " + stringBuilderSds.toString());
//                    }
//
//                    if (totalBytes >= 4){
//
//                        byte le = data[1];
//                        byte ler = data[2];
//
//                        if (le == ler){
//
//                            if (totalBytes >= (le & 0xFF) + 6){
//
//                                byte  sd2 = data[3];
//
//                                if (sd2 == UsbTelegram.SD2){
//
//                                    ed = data[(le & 0xFF) + 5];
//
//                                    if (ed == UsbTelegram.ED){
//
//                                        for (int i = 0; i < (le & 0xFF); i++){
//                                            fcsCalc += data[i + 4];
//                                        }
//                                        fcsRead = data[(le & 0xFF) + 4];
//
//                                        if (fcsCalc  == fcsRead){
//
//                                            da = data[4];
//                                            sa = data[5];
//                                            fc = data[6];
//
//                                            if ((da & 0x80) == 0x80 && (sa & 0x80) == 0x80){
//                                                dsap = data[7];
//                                                ssap = data[8];
//                                                du = Arrays.copyOfRange(data, 9, (le & 0xFF) + 4);
//
//                                                usbTelegramList.add(new UsbTelegram(sd, da, sa, fc, dsap, ssap, du));
//
//                                                readIndex += (le & 0xFF) + 6;
//                                                readIndex %= maxnumbytes;
//                                                totalBytes -= (le & 0xFF) + 6;
//                                            }
//                                            else {
//                                                du = Arrays.copyOfRange(data, 7, (le & 0xFF) + 4);
//
//                                                usbTelegramList.add(new UsbTelegram(sd, da, sa, fc, dsap, ssap, du));
//
//                                                readIndex += (le & 0xFF) + 6;
//                                                readIndex %= maxnumbytes;
//                                                totalBytes -= (le & 0xFF) + 6;
//                                            }
//                                        }
//                                        else {
//                                            MyLog.e(TAG, "Frame check sequence wrong (read: " + fcsRead + ", calculated: " + fcsCalc + ")");
//
//                                            readIndex++;
//                                            readIndex %= maxnumbytes;
//                                            totalBytes--;
//                                        }
//                                    }
//                                    else {
//                                        MyLog.e(TAG, "Missing or wrong end delimiter (ed: " + ed + ")");
//
//                                        readIndex++;
//                                        readIndex %= maxnumbytes;
//                                        totalBytes--;
//                                    }
//                                }
//                                else {
//                                    MyLog.e(TAG, "Missing start delimiter 2 (sd: " + sd2 + ")");
//
//                                    readIndex++;
//                                    readIndex %= maxnumbytes;
//                                    totalBytes--;
//                                }
//                            }
//                            else {
////                                MyLog.w(TAG, "Total bytes in buffer is shorter than the SD2 telegram length (Total bytes: " + totalBytes + " | SD2 length: " + (le & 0xFF) + "+6)");
//                                dataAvailable = false;
//                            }
//                        }
//                        else {
//                            MyLog.e(TAG, "Read length and length repeated doesn't match (le: " + (le & 0xFF) + ", ler: " + (ler & 0xFF) + ")");
//
//                            readIndex++;
//                            readIndex %= maxnumbytes;
//                            totalBytes--;
//                        }
//                    }
//                    else {
////                        MyLog.w(TAG, "Total bytes in buffer is shorter than the minimum SD2 telegram length (Total bytes: " + totalBytes + " | SD2 minimum length: 9)");
//                        dataAvailable = false;
//                    }
//
//                    break;
//
//                case UsbTelegram.SD3:
//
//                    if (totalBytes >= 14){
//
//                        da = data[1];
//                        sa = data[2];
//                        fc = data[3];
//                        du = Arrays.copyOfRange(data, 4, 12);
//                        fcsRead = data[12];
//                        ed = data[13];
//
//                        if (ed == UsbTelegram.ED){
//
//                            fcsCalc = 0;
//                            for (int i = 0; i < 11; i++){
//                                fcsCalc += data[i + 1];
//                            }
//
//                            if (fcsCalc == fcsRead){
//                                usbTelegramList.add(new UsbTelegram(sd, da, sa, fc, dsap, ssap, du));
//
//                                readIndex += 14;
//                                readIndex %= maxnumbytes;
//                                totalBytes -= 14;
//                            }
//                            else {
//                                MyLog.e(TAG, "Frame check sequence wrong (read: " + fcsRead + ", calculated: " + fcsCalc + ")");
//
//                                readIndex++;
//                                readIndex %= maxnumbytes;
//                                totalBytes--;
//                            }
//                        }
//                        else {
//                            MyLog.e(TAG, "Missing or wrong end delimiter (ed: " + ed + ")");
//
//                            readIndex++;
//                            readIndex %= maxnumbytes;
//                            totalBytes--;
//                        }
//                    }
//                    else {
////                        MyLog.w(TAG, "Total bytes in buffer is shorter than the SD3 telegram length (Total bytes: " + totalBytes + ", SD3 length: 14)");
//                        dataAvailable = false;
//                    }
//
//                    break;
//
//                case UsbTelegram.SD4:
//
//                    if (totalBytes >= 3){
//
//                        da = data[1];
//                        sa = data[2];
//
//                        usbTelegramList.add(new UsbTelegram(sd, da, sa, fc, dsap, ssap, null));
//
//                        readIndex += 3;
//                        readIndex %= maxnumbytes;
//                        totalBytes -= 3;
//                    }
//                    else {
////                        MyLog.w(TAG, "Total bytes in buffer is shorter than the SD4 telegram length (Total bytes: " + totalBytes + ", SD4 length: 3)");
//                        dataAvailable = false;
//                    }
//
//                    break;
//
//                case UsbTelegram.SC:
//
//                    usbTelegramList.add(new UsbTelegram(sd, da, sa, fc, dsap, ssap, null));
//
//                    readIndex++;
//                    readIndex %= maxnumbytes;
//                    totalBytes--;
//
//                    break;
//
//                default:
//
//                    readIndex++;
//                    readIndex %= maxnumbytes;
//                    totalBytes--;
//
//                    stringBuilderSds.append(Utilities.byteToHex(sd));
//                    stringBuilderSds.append(" ");
//            }
        }

        return status;
    }

    /**
     * Write the usb write buffer to the output stream.
     *
     * @param numBytes number of bytes to write
     */
    private void SendPacket(int numBytes) {
        try {
            if(outputstream != null){
                outputstream.write(writeusbdata, 0, numBytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Resumes accessory
     *
     * @return
     */
    public int ResumeAccessory() {
        // Intent intent = getIntent();
        if (inputstream != null && outputstream != null) {
            return 1;
        }

        UsbAccessory[] accessories = usbmanager.getAccessoryList();
        boolean accessory_attached = false;
        if(accessories != null)
        {
            Toast.makeText(global_context, "Accessory Attached", Toast.LENGTH_SHORT).show();
        }
        else
        {
            // return 2 for accessory detached case
            //MyLog.e(">>@@","ResumeAccessory RETURN 2 (accessories == null)");
            accessory_attached = false;
            return 2;
        }

        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            String manufacturerString = "mManufacturer=FTDI";
            if(!accessory.toString().contains(manufacturerString))
            {
                Toast.makeText(global_context, "Manufacturer is not matched!", Toast.LENGTH_SHORT).show();
                return 1;
            }

            String modelString2 = "mModel=Android Accessory FT312D";
            String modelString1 = "mModel=FTDIUARTDemo";
            if(!accessory.toString().contains(modelString1) && !accessory.toString().contains(modelString2))
            {
                Toast.makeText(global_context, "Model is not matched!", Toast.LENGTH_SHORT).show();
                return 1;
            }

            String versionString = "mVersion=1.0";
            if(!accessory.toString().contains(versionString))
            {
                Toast.makeText(global_context, "Version is not matched!", Toast.LENGTH_SHORT).show();
                return 1;
            }

            Toast.makeText(global_context, "Manufacturer, Model & Version are matched!", Toast.LENGTH_SHORT).show();
            accessory_attached = true;

            if (usbmanager.hasPermission(accessory)) {
                OpenAccessory(accessory);
            }
            else
            {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        Toast.makeText(global_context, "Request USB Permission", Toast.LENGTH_SHORT).show();
                        usbmanager.requestPermission(accessory,
                                mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {}

        return 0;
    }

    /**
     * Destroys accessory
     *
     * @param bConfiged
     */
    public void DestroyAccessory(boolean bConfiged) {

        if(bConfiged){
            read_enable = false;  // set false condition for HandlerThread to exit waiting data loop
            writeusbdata[0] = 0;  // send dummy data for instream.read going
            SendPacket(1);
        }
        else
        {
            SetDefaultConfig();  // send default setting data for config
            try{Thread.sleep(10);}
            catch(Exception e){}

            read_enable = false;  // set false condition for HandlerThread to exit waiting data loop
            writeusbdata[0] = 0;  // send dummy data for instream.read going
            SendPacket(1);
        }

        try{Thread.sleep(10);}
        catch(Exception e){}
        CloseAccessory();
    }

    /**
     * Opens accessory
     *
     * @param accessory
     */
    private void OpenAccessory(UsbAccessory accessory) {
        filedescriptor = usbmanager.openAccessory(accessory);
        if(filedescriptor != null){
            UsbAccessory usbaccessory = accessory;

            FileDescriptor fd = filedescriptor.getFileDescriptor();

            inputstream = new FileInputStream(fd);
            outputstream = new FileOutputStream(fd);
            /*check if any of them are null*/
            if(inputstream == null || outputstream==null){
                return;
            }

            if(!read_enable){
                read_enable = true;
                read_thread readThread = new read_thread(inputstream);
                readThread.start();
            }
        }
    }

    /**
     * Closes accessory
     */
    public void CloseAccessory() {
        try{
            if(filedescriptor != null)
                filedescriptor.close();

        }catch (IOException e){}

        try {
            if(inputstream != null)
                inputstream.close();
        } catch(IOException e){}

        try {
            if(outputstream != null)
                outputstream.close();

        }catch(IOException e){}

        filedescriptor = null;
        inputstream = null;
        outputstream = null;

//        System.exit(0);
    }

    /**
     * USB broadcast receiver
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action))
            {
                synchronized (this)
                {
                    UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                    {
                        Toast.makeText(global_context, "Allow USB Permission", Toast.LENGTH_SHORT).show();
                        OpenAccessory(accessory);
                    }
                    else
                    {
                        Toast.makeText(global_context, "Deny USB Permission", Toast.LENGTH_SHORT).show();
                        MyLog.d("LED", "permission denied for accessory " + accessory);

                    }
                    mPermissionRequestPending = false;
                }
            }
            else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action))
            {
                read_enable = false;
                context.unregisterReceiver(mUsbReceiver);
                disconnected = true;
                Utilities.displayToast(global_context, "USB disconnected! Please close the app before reconnecting!");
                MyLog.w(TAG, "USB disconnected! Please close the app before reconnecting!");
//                DestroyAccessory(true);
//                CloseAccessory();
            }
        }
    };

    /**
     * Thread that reads the incoming data from the in stream.
     */
    private class read_thread extends Thread {
        FileInputStream instream;

        read_thread(FileInputStream stream ){
            instream = stream;
            this.setPriority(Thread.MAX_PRIORITY);
        }

        public void run()
        {
            while(read_enable)
            {
                while(totalBytes >= (maxnumbytes - 2048))
                {
                    try
                    {
                        MyLog.e("read_thread", "Buffer full, sleep for 50ms (total bytes in buffer: " + totalBytes + " | writeIndex: " + writeIndex + " | readIndex: " + readIndex + ")");
                        Thread.sleep(50);
                    }
                    catch (InterruptedException e) {e.printStackTrace();}
                }

                try
                {
                    if(instream != null)
                    {
                        int readCount = instream.read(usbdata, 0, 1024);
                        if(readCount > 0)
                        {
                            for(int count = 0; count< readCount; count++)
                            {
                                readBuffer[writeIndex] = usbdata[count];
                                writeIndex++;
                                writeIndex %= maxnumbytes;
                            }

                            if(writeIndex >= readIndex)
                                totalBytes = writeIndex - readIndex;
                            else
                                totalBytes = (maxnumbytes - readIndex) + writeIndex;
                        }
                    }
                }
                catch (IOException e){
//                    e.printStackTrace();
                }
            }
        }
    }
}
