package se.grunka.jeeves.sample;

import se.grunka.jeeves.client.ServiceClient;
import se.grunka.jeeves.sample.api.HelloWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MyServiceClient {
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        final HelloWorld helloWorld = ServiceClient.create(HelloWorld.class, "http://localhost:8080/test/rpc");
        ExecutorService executorService = Executors.newFixedThreadPool(100, new ThreadFactory() {
            private final ThreadFactory delegate = Executors.defaultThreadFactory();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = delegate.newThread(r);
                thread.setDaemon(true);
                return thread;
            }
        });
        List<Future<String>> futures = new ArrayList<Future<String>>();
        for (int i = 0; i < 1000000; i++) {
            futures.add(executorService.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return helloWorld.helloWho("You");
                }
            }));
        }
        for (Future<String> future : futures) {
            String result = future.get();
            boolean correct = "Hello You!".equals(result);
            assert correct;
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("duration = " + duration);
    }
}
