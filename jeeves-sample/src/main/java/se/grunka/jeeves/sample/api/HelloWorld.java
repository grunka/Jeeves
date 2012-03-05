package se.grunka.jeeves.sample.api;

import se.grunka.jeeves.Method;
import se.grunka.jeeves.Param;
import se.grunka.jeeves.Service;

@Service("service")
public interface HelloWorld {
    @Method("hello")
    String helloWorld();
    @Method("hello")
    String helloWho(@Param("name") String who);
}
