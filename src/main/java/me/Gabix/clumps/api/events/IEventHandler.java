package me.Gabix.clumps.api.events;

public interface IEventHandler<T, U> {
    U handle(T var1);
}