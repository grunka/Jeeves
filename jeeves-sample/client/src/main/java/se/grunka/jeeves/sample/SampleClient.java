package se.grunka.jeeves.sample;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import se.grunka.jeeves.JeevesClient;

public class SampleClient {
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        final HelloWorld helloWorld = JeevesClient.create(HelloWorld.class, "http://localhost:8080/rpc");
        ExecutorService executorService = Executors.newFixedThreadPool(200);
        for (int i = 0; i < 100000; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    helloWorld.helloWho("You");
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);
        long duration = System.currentTimeMillis() - start;
        System.out.println("duration = " + duration);
    }
}
