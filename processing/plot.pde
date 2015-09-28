import processing.serial.*;

Serial port;
final int bsize = 900; // sample rate * seconds
final int scale = 2;
int[] buffer1 = new int[bsize];
int[] buffer2 = new int[bsize];
int[] graph = new int[bsize];
static volatile boolean buf1full = false;
static volatile boolean buf2full = false;

void setup() {
  size(bsize*scale, 1030); // maximum value 1023 (2^10-1)
  background(0);
  stroke(255);
  smooth();
  
  // check serial list first
  port = new Serial(this, Serial.list()[0], 57600);
  thread("background");
}

void draw() {
  if (buf1full) {
    for (int i = 0; i < bsize; i++) {
      graph[i] = buffer1[i];
    }
    buf1full = false;
    plot();
    
  } else if (buf2full) {
    for (int i = 0; i < bsize; i++) {
      graph[i] = buffer2[i];
    }
    buf2full = false;
    plot();
  }
}

void plot() {
  background(0);
  for (int i = 1; i < bsize; i++) {
    line((i-1)*scale, graph[i-1],
         i*scale, graph[i]);
  }
}

void background() {
  while (true) {
    if (!buf1full) {
      int i = 0;
      while (i < bsize) {
        if (port.available() >= 2) {
          buffer1[i] = ((port.read() << 8) | (port.read()));
          i++;
        } else {
          try {
            Thread.sleep(2); // (1000/sample rate)/3
          } catch (InterruptedException e) { e.printStackTrace(); }
        }
      }
      buf1full = true;
      
    } else if (!buf2full) {
      int i = 0;
      while (i < bsize) {
        if (port.available() >= 2) {
          buffer2[i] = ((port.read() << 8) | (port.read()));
          i++;
        } else {
          try {
            Thread.sleep(2);
          } catch (InterruptedException e) { e.printStackTrace(); }
        }
      }
      buf2full = true;
    }
  }
}

void keyPressed() {
  exit();
}
