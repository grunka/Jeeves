package se.grunka.jeeves.sample;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.grunka.jeeves.JeevesClient;

public class SampleClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleClient.class);

    public static void main(String[] args) throws Exception {
        final HelloWorld helloWorld = JeevesClient.create(HelloWorld.class, "http://localhost:8080/rpc");
        ExecutorService executorService = Executors.newFixedThreadPool(200);
        long start = System.currentTimeMillis();
        LOGGER.info("Starting");
        for (int i = 0; i < 10000; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    String result = helloWorld.helloWho("You");
                    if (!"Hello You!".equals(result)) {
                        LOGGER.error("Did not get the expected result");
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);
        long duration = System.currentTimeMillis() - start;
        LOGGER.info("Completed after " + duration + "ms");
    }
}
