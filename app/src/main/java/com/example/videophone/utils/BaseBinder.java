package com.example.videophone.utils;

import android.os.Binder;

public class BaseBinder<T> extends Binder {
    public Object value;

    private Class<T> clazz;

    public BaseBinder() {
    }

    public BaseBinder(Object value, Class<T> clazz) {
        this.value = value;
        this.clazz = clazz;
    }

    public T value() {
        return Converter.convertInstanceOfObject(value, clazz);
    }
}