package se.grunka.jeeves.sample;

import se.grunka.jeeves.Method;
import se.grunka.jeeves.Param;
import se.grunka.jeeves.Service;

@Service("service")
public interface Complex {
    @Method("complex")
    String complex(@Param("value") ComplexValue value);
}
