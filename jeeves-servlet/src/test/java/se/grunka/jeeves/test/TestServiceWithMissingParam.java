package se.grunka.jeeves.test;

import se.grunka.jeeves.Method;
import se.grunka.jeeves.Param;
import se.grunka.jeeves.Service;

@Service("service")
public interface TestServiceWithMissingParam {
    @Method("method")
    void method(@Param("one") String one, String two);
}
