/*
 * JPty - A small PTY interface for Java.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package jpty.linux;

import jpty.JPty.JPtyInterface;
import jpty.WinSize;
import jtermios.Termios;
import jtermios.linux.JTermiosImpl.Linux_C_lib.termios;

import com.sun.jna.Native;
import com.sun.jna.StringArray;
import com.sun.jna.Structure;

/**
 * Provides the PTY specific functions for Linux.
 */
public class JPtyImpl implements JPtyInterface {
    // INNER TYPES
    
    public static class winsize extends Structure {
        public short ws_row;
        public short ws_col;
        public short ws_xpixel;
        public short ws_ypixel;
        
        public winsize() {
        }
        
        public winsize(WinSize ws) {
            ws_row = ws.ws_row;
            ws_col = ws.ws_col;
            ws_xpixel = ws.ws_xpixel;
            ws_ypixel = ws.ws_ypixel;
        }

        public void update(WinSize winSize) {
            winSize.ws_col = ws_col;
            winSize.ws_row = ws_row;
            winSize.ws_xpixel = ws_xpixel;
            winSize.ws_ypixel = ws_ypixel;
        }
    }
    
    public interface Linux_Util_lib extends com.sun.jna.Library {
        public int forkpty(int[] amaster, byte[] name, termios termp, winsize winp);
    }
    
    public interface Linux_C_lib extends com.sun.jna.Library {
        public int execve(String command, StringArray argv, StringArray env);

        public int ioctl(int fd, int cmd, winsize arg);

        public int waitpid(int pid, int[] stat, int options);
    }
    
    // CONSTANTS
    
    private final static int TIOCGWINSZ  = 0x00005413;
    private final static int TIOCSWINSZ  = 0x00005414;

    // VARIABLES
    
    private static Linux_C_lib m_Clib = (Linux_C_lib) Native.loadLibrary("c", Linux_C_lib.class);
    private static Linux_Util_lib m_Utillib = (Linux_Util_lib) Native.loadLibrary("util", Linux_Util_lib.class);

    // METHODS
    
    @Override
    public int execve(String command, String[] argv, String[] env) {
        StringArray argvp = (argv == null) ? new StringArray(new String[] { command }) : new StringArray(argv);
        StringArray envp = (env == null) ? new StringArray(new String[0]) : new StringArray(env);
        return m_Clib.execve(command, argvp, envp);
    }
    
    @Override
    public int forkpty(int[] amaster, byte[] name, Termios term, WinSize win) {
        termios termp = (term == null) ? null : new termios(term);
        winsize winp = (win == null) ? null : new winsize(win);
        return m_Utillib.forkpty(amaster, name, termp, winp);
    }
    
    @Override
    public int getWinSize(int fd, WinSize winSize) {
        int r;
        
        winsize ws = new winsize();
        if ((r = m_Clib.ioctl(fd, TIOCGWINSZ, ws)) < 0) {
            return r;
        }
        ws.update(winSize);

        return r;
    }

    @Override
    public int setWinSize(int fd, WinSize winSize) {
        winsize ws = new winsize(winSize);
        return m_Clib.ioctl(fd, TIOCSWINSZ, ws);
    }
    
    @Override
    public int waitpid(int pid, int[] stat, int options) {
        return m_Clib.waitpid(pid, stat, options);
    }
}
