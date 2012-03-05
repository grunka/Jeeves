package se.grunka.jeeves.test;

import se.grunka.jeeves.Method;
import se.grunka.jeeves.Param;
import se.grunka.jeeves.Service;

@Service("service")
public interface TestServiceWithMultipleParameters {
    @Method("method")
    String method(@Param("a") String a, @Param("b") String b);
}
