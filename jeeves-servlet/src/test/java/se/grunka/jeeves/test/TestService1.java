package se.grunka.jeeves.test;

import se.grunka.jeeves.Method;
import se.grunka.jeeves.Param;
import se.grunka.jeeves.Service;

@Service("service-one")
public interface TestService1 {
    @Method("method-one")
    String method1(@Param("parameter-one") String param);
    void notAServiceMethod();
}
