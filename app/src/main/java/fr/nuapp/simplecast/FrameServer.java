package fr.nuapp.simplecast;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Experimental LAN-only JPEG frame server. No authentication: trusted Wi-Fi ONLY. */
public class FrameServer {
    private final Supplier<byte[]> frame;
    private ServerSocket socket;
    private final ExecutorService workers = Executors.newFixedThreadPool(4);
    private Thread acceptThread;
    FrameServer(Supplier<byte[]> frame) { this.frame = frame; }
    void start() throws IOException {
        socket = new ServerSocket(8765);
        acceptThread = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    Socket client = socket.accept(); client.setSoTimeout(4000);
                    workers.execute(() -> handle(client));
                } catch (IOException e) { if (!socket.isClosed()) android.util.Log.w("SimpleCast", "Accept", e); }
            }
        });
        acceptThread.start();
    }
    private void handle(Socket client) {
        try (Socket s = client; BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream())); OutputStream out = s.getOutputStream()) {
            String request = in.readLine(); if (request == null) return;
            String path = request.split(" ")[1];
            if (!path.equals("/") && !path.startsWith("/frame.jpg")) { respond(out,404,"text/plain", "Not found".getBytes(StandardCharsets.UTF_8)); return; }
            if (path.startsWith("/frame.jpg")) {
                byte[] bytes = frame.get();
                if (bytes == null) respond(out,503,"text/plain", "Waiting for first frame".getBytes(StandardCharsets.UTF_8));
                else respond(out,200,"image/jpeg",bytes);
            } else {
                String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>body{margin:0;background:#101820;display:grid;place-items:center;height:100vh}img{max-width:100vw;max-height:100vh;object-fit:contain}</style></head><body><img id='screen' alt='Ecran Android'><script>const img=document.querySelector('#screen');function tick(){const next=new Image();next.onload=()=>{img.src=next.src;setTimeout(tick,250)};next.onerror=()=>setTimeout(tick,700);next.src='/frame.jpg?ts='+Date.now()}tick()</script></body></html>";
                respond(out,200,"text/html; charset=utf-8",html.getBytes(StandardCharsets.UTF_8));
            }
        } catch(Exception ignored) { }
    }
    private void respond(OutputStream out,int code,String type,byte[] body) throws IOException {
        String status = code==200?"OK":code==404?"Not Found":"Service Unavailable";
        String header="HTTP/1.1 "+code+" "+status+"\r\nContent-Type: "+type+"\r\nContent-Length: "+body.length+"\r\nCache-Control: no-store\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.US_ASCII)); out.write(body); out.flush();
    }
    void shutdown() { try { if(socket!=null)socket.close(); }catch(IOException ignored){} workers.shutdownNow(); }
}
