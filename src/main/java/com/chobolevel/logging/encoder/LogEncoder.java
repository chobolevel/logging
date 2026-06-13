package com.chobolevel.logging.encoder;

import com.chobolevel.logging.core.LogRecord;

public interface LogEncoder {
    String encode(LogRecord record);
}