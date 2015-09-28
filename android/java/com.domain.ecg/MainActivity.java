package com.domain.ecg;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import java.util.HashMap;

public class MainActivity extends Activity {

  private final int bsize = 1280; // 2 * sample rate * seconds
  private final int scale = 2;
  private byte[] buffer1 = new byte[bsize];
  private byte[] buffer2 = new byte[bsize];
  private int[] graph = new int[bsize/2];
  private static volatile boolean buf1full = false;
  private static volatile boolean buf2full = false;
  UsbDevice device;
  UsbDeviceConnection connection;
  
  private static volatile boolean run = false;
  Thread thread;

  ImageView imageView;
  Canvas canvas;
  Paint paint;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    imageView = (ImageView) this.findViewById(R.id.imageView);
    //  maximum value 1023 (2^10-1)
    Bitmap bitmap = Bitmap.createBitmap(graph.length*scale, 1030, Bitmap.Config.ARGB_8888);
    canvas = new Canvas(bitmap);
    imageView.setImageBitmap(bitmap);
    paint = new Paint();
    paint.setStrokeWidth(3);
    paint.setColor(Color.BLACK);
  }

  @Override
  protected void onStart() {
    super.onStart();

    UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
    HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
    for (int i = 0; i < deviceList.entrySet().size(); i++) {
      UsbDevice tmp = deviceList.entrySet().iterator().next().getValue();
      if (tmp.getVendorId() == 9025 && tmp.getProductId() == 67) device = tmp;
    }
    if (device != null) {
      connection = manager.openDevice(device);
      connection.claimInterface(device.getInterface(1), true);
      connection.controlTransfer(0x21, 0x22, 0, 0, null, 0, 0);
      connection.controlTransfer(0x21, 0x20, 0, 0, getLineEncoding(57600), 7, 0);
      ((Button) findViewById(R.id.button)).setEnabled(true);
    }
    
    update(true);
  }

  public void btnClick(View view) {
    if (!run) {
      run = true;
      thread = new Thread(new Task());
      thread.start();
    } else {
      run = false;
    }
  }

  public void update(boolean oneOrTwo) {
        int tmp1; int tmp2;
        int bi = 0;
    if (oneOrTwo) {
      for (int gi = 0; gi < graph.length; gi++) {
        if (buffer1[bi] < 0) tmp1 = buffer1[bi] + 256;
        else tmp1 = buffer1[bi];
        if (buffer1[bi+1] < 0) tmp2 = buffer1[bi+1] + 256;
        else tmp2 = buffer1[bi+1];
        graph[gi] = ((tmp1 << 8) | tmp2);
        bi += 2;
      }
      buf1full = false;
            
    } else {
      for (int gi = 0; gi < graph.length; gi++) {
        if (buffer2[bi] < 0) tmp1 = buffer2[bi] + 256;
        else tmp1 = buffer2[bi];
        if (buffer2[bi+1] < 0) tmp2 = buffer2[bi+1] + 256;
        else tmp2 = buffer2[bi+1];
        graph[gi] = ((tmp1 << 8) | tmp2);
        bi += 2;
      }
      buf2full = false;
    }
    
    plot();
    }
    
  private void plot() {
    canvas.drawColor(Color.WHITE);
    for (int i = 1; i < graph.length; i++) {
      canvas.drawLine((i-1)*scale, graph[i-1],
                      i*scale, graph[i], paint);
    }
    imageView.invalidate();
  }

  private byte[] getLineEncoding(int baudRate) {
    final byte[] lineEncodingRequest = { (byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08 };
    lineEncodingRequest[0] = (byte)(baudRate & 0xFF);
    lineEncodingRequest[1] = (byte)((baudRate >> 8) & 0xFF);
    lineEncodingRequest[2] = (byte)((baudRate >> 16) & 0xFF);
    return lineEncodingRequest;
  }

  class Task implements Runnable {
    @Override
    public void run() {
      try { Thread.sleep(2000); } catch (Exception e) { }
      byte[] bytes = new byte[64];
      int transferred = 0;
      int i; int bi = 0;
      boolean oneOrTwo = true;

      while (run) {
        transferred = connection.bulkTransfer(device.getInterface(1).getEndpoint(1), bytes, bytes.length, 10000);
        i = 0;
        while (i < transferred) {
          if (oneOrTwo && !buf1full) {
            while (i < transferred && bi < bsize) {
              buffer1[bi] = bytes[i];
                i++;
                bi++;
            }
            if (bi == bsize) {
              buf1full = true;
              MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  MainActivity.this.update(true);
                }
              });
              oneOrTwo = false;
              bi = 0;
            }
            
          } else if (!oneOrTwo && !buf2full) {
            while (i < transferred && bi < bsize) {
              buffer2[bi] = bytes[i];
                i++;
                bi++;
            }
            if (bi == bsize) {
              buf2full = true;
              MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  MainActivity.this.update(false);
                }
              });
              oneOrTwo = true;
              bi = 0;
            }
          }
        }
        if (transferred < 1) {
          try { Thread.sleep(2); } catch (Exception e) { }  // (1000/sample rate)/3
        }
      }
    }
  }
}
