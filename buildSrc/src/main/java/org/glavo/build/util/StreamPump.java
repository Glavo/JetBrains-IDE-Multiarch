/*
 * Copyright 2025 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glavo.build.util;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class StreamPump implements Runnable {

    public static void pump(Process process, Logger logger) {
        new Thread(new StreamPump(process.getInputStream(), logger, LogLevel.LIFECYCLE)).start();
        new Thread(new StreamPump(process.getErrorStream(), logger, LogLevel.ERROR)).start();
    }

    private final InputStream inputStream;
    private final Logger logger;
    private final LogLevel level;

    public StreamPump(InputStream inputStream, Logger logger, LogLevel level) {
        this.inputStream = inputStream;
        this.logger = logger;
        this.level = level;
    }

    @Override
    public void run() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    break;
                }
                logger.log(level, line);
            }
        } catch (IOException e) {
            logger.error("An error occurred when reading stream", e);
        }
    }

}

