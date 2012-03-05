package se.grunka.jeeves.test;

import se.grunka.jeeves.Method;
import se.grunka.jeeves.Param;
import se.grunka.jeeves.Service;

@Service("service")
public interface TestServiceB {
    @Method("method")
    void methodTwo(@Param("two") int two);
}
