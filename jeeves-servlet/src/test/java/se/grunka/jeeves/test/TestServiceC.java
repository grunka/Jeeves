package se.grunka.jeeves.test;

import se.grunka.jeeves.Method;
import se.grunka.jeeves.Param;
import se.grunka.jeeves.Service;

@Service("service")
public interface TestServiceC {
    @Method("method")
    String methodThree(@Param("one") String one, @Param("three") double three);
}
