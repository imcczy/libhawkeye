/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Jonathan Bardin     <astrosus@gmail.com>
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package com.ibm.wala.dalvik.classLoader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Section;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.util.io.TemporaryFile;

/**
 * A module which is a wrapper around .dex and .apk file.
 *
 * @author barjo
 */
public class DexFileModule implements Module {
    private final DexFile dexfile;
    private final Collection<ModuleEntry> entries;

    public static DexFileModule make(File f) throws IllegalArgumentException, IOException {
    	if (f.getName().endsWith("jar")) {
    		try (final JarFile jar = new JarFile(f)) {
    			return new DexFileModule(jar);
    		}
    	} else {
    		return new DexFileModule(f);
    	}
    }
    /*
     * added by imcczy
     */
    public static DexFileModule make(Path path) throws IllegalArgumentException, IOException {
        if (path.getFileName().toString().endsWith("jar")){
            try (final JarFile jar = new JarFile(path.toFile())) {
                return new DexFileModule(jar);
            }
        }else {
            return new DexFileModule(path);
        }
    }
    
    private static File tf(JarFile f) {
    	String name = f.getName();
    	if (name.indexOf('/') >= 0) {
    		name = name.substring(name.lastIndexOf('/')+1);
    	}
    	File tf = new File(System.getProperty("java.io.tmpdir") + "/" + name + "_classes.dex");
    	tf.deleteOnExit();
    	//System.err.println("using " + tf);
    	return tf;
    }
    
    private DexFileModule(JarFile f) throws IllegalArgumentException, IOException {    	
    	this(TemporaryFile.streamToFile(tf(f), f.getInputStream(f.getEntry("classes.dex"))));
    }
    
    /**
     * @param file
     *            the .dex or .apk file
     * @throws IllegalArgumentException
     */
    private DexFileModule(File file) throws IllegalArgumentException {
        try {
            dexfile = new DexFile(file);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        // create ModuleEntries from ClassDefItem
        entries = new HashSet<>();

        Section<ClassDefItem> cldeff = dexfile.ClassDefsSection;
        for (ClassDefItem cdefitems : cldeff.getItems()) {
            entries.add(new DexModuleEntry(cdefitems));
        }
    }
/*
 *added by imcczy
 */
    private DexFileModule(Path path) throws IllegalArgumentException,IOException{
        List<Path> list = null;
        try (FileSystem fileSystem = FileSystems.newFileSystem(path,null)){
            list = Files.list(fileSystem.getPath("/"))
                    .filter(zipPath -> zipPath.toString().endsWith(".dex"))
                    .collect(Collectors.toList());
        }
        // create ModuleEntries from ClassDefItem
        entries = new HashSet<>();
        dexfile = null;
        try (ZipFile zipFile = new ZipFile(path.toFile())){
            for (Path p : list){
                ZipEntry zipEntry = zipFile.getEntry(p.getFileName().toString());
                if (zipEntry == null) {
                    throw new DexFile.NoClassesDexException("zip file " + path.getFileName() + " does not contain a classes.dex " +
                            "file");
                }
                long fileLength = zipEntry.getSize();
                if (fileLength < 40) {
                    throw new RuntimeException("The classes.dex file in " + path.getFileName() + " is too small to be a" +
                            " valid dex file");
                } else if (fileLength > Integer.MAX_VALUE) {
                    throw new RuntimeException("The classes.dex file in " + path.getFileName() + " is too large to read in");
                }
                InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                DexFile dexfile = new DexFile(inputStream,fileLength);
                Section<ClassDefItem> cldeff = dexfile.ClassDefsSection;
                for (ClassDefItem cdefitems : cldeff.getItems()) {
                    entries.add(new DexModuleEntry(cdefitems));
                }
                dexfile = null;
            }
        }
    }

    /**
     * @return The DexFile associated to this module.
     */
    public DexFile getDexFile() {
        return dexfile;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wala.classLoader.Module#getEntries()
     */
    public Iterator<ModuleEntry> getEntries() {
        return entries.iterator();
    }

    @Override
    public Collection<ModuleEntry> getEntrysCollection(){
        return entries;
    }

    private static boolean isMultiDex() throws IOException{
        return true;
    }

}
