package me.Gabix.core;

/**
 * Duck interface implemented by {@link me.Gabix.core.mixin.WindowMixin}.
 * It lives outside the mixin package on purpose: classes inside a package
 * owned by a mixin config cannot be referenced directly at runtime.
 */
public interface WindowAccess {
    int mvp$getRealFramebufferWidth();

    int mvp$getRealFramebufferHeight();

    double mvp$getRealScaleFactor();
}
