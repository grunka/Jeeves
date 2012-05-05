package se.grunka.jeeves.sample;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import se.grunka.jeeves.JeevesClient;

public class MyServiceClient {
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        final HelloWorld helloWorld = JeevesClient.create(HelloWorld.class, "http://localhost:8080/rpc");
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10000; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    helloWorld.helloWho("You");
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);
        /*
        for (Future<String> future : futures) {
            String result = future.get();
            boolean correct = "Hello You!".equals(result);
            System.out.println("blopp");
            assert correct;
        }
        */
        long duration = System.currentTimeMillis() - start;
        System.out.println("duration = " + duration);
    }
}
