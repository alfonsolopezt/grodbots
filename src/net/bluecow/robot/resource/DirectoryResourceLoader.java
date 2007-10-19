/*
 * Copyright (c) 2007, Jonathan Fuerth
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of Jonathan Fuerth nor the names of other
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * Created on Sep 26, 2006
 *
 * This code belongs to SQL Power Group Inc.
 */
package net.bluecow.robot.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DirectoryResourceLoader extends AbstractResourceLoader {

    private File basedir;
    
    public DirectoryResourceLoader(File basedir) {
        if (!basedir.isDirectory()) {
            if (!basedir.exists()) {
                throw new IllegalArgumentException(
                        "The given base directory '"+basedir.getAbsolutePath()+
                        "' does not exist");
            } else {
                throw new IllegalArgumentException(
                        "The given base directory '"+basedir.getAbsolutePath()+
                        " is not a directory");
            }
        }
        this.basedir = basedir;
    }
    
    public InputStream getResourceAsStream(String resourceName) throws IOException {
        File resourceFile = new File(basedir, resourceName);
        if (!resourceFile.exists()) {
            throw new FileNotFoundException("Resource file '"+resourceFile.getAbsolutePath()+"' not found");
        }
        if (!resourceFile.canRead()) {
            throw new IOException("Resource file '"+resourceFile.getAbsolutePath()+"' exists but is not readable");
        }
        return new FileInputStream(resourceFile);
    }

}