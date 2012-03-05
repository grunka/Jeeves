package se.grunka.jeeves.sample.api;

import se.grunka.jeeves.Method;
import se.grunka.jeeves.Param;
import se.grunka.jeeves.Service;

@Service("service")
public interface MathOperations {
    @Method("add")
    int add(@Param("x") int x, @Param("y") int y);
}
