package se.grunka.jeeves.test;

import se.grunka.jeeves.Method;
import se.grunka.jeeves.Param;
import se.grunka.jeeves.Service;

@Service("service")
public interface TestServiceWithDuplicateDefinition {
    @Method("method")
    void method1(@Param("one") int one);
    @Method("method")
    void method2(@Param("one") int one);
}
