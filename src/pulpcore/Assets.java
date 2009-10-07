/*
    Copyright (c) 2007-2009, Interactive Pulp, LLC
    All rights reserved.
    
    Redistribution and use in source and binary forms, with or without 
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright 
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright 
          notice, this list of conditions and the following disclaimer in the 
          documentation and/or other materials provided with the distribution.
        * Neither the name of Interactive Pulp, LLC nor the names of its 
          contributors may be used to endorse or promote products derived from 
          this software without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
    POSSIBILITY OF SUCH DAMAGE.
*/

package pulpcore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import pulpcore.platform.AppContext;
import pulpcore.platform.AssetCatalog;
import pulpcore.util.ByteArray;

/**
    The Assets class provided a central location to retrieve game assets
    (images, fonts, sounds, etc.) from the jar or from zip files.
*/
public class Assets {
    
    // Lock for multiple app contexts running simultaneously.
    private static final Object LOCK = new Object();

    // Prevent instantiation
    private Assets() { }

    // List<AssetCatalog>
    private static List getContextCatalogs() {
        AppContext context = CoreSystem.getThisAppContext();
        if (context == null) {
            return null;
        }

        return context.getAssetCatalogs();
    }
    
    /**
        Adds the contents of an asset catalog (zip file) into memory.
        <p>
        If this catalog contains an asset that has the same name as an existing asset, 
        the old asset is replaced with the new one. If this zip file contains errors,
        no existing assets are affected and this method returns false.
        
        @param zipFileData a zip file
        @return true on success; false otherwise.
    */
    public static boolean addCatalog(String catalogName, byte[] zipFileData) {
        if (zipFileData == null || zipFileData.length == 0) {
            return false;
        }
        
        return addCatalog(catalogName, new ByteArrayInputStream(zipFileData));
    }
    
    /**
        Adds the contents of an asset catalog (zip file) into memory.
        <p>
        If this catalog contains an asset that has the same name as an existing asset, 
        the old asset is replaced with the new one. If this zip file contains errors,
        no existing assets are affected and this method returns false.
        
        @param is an input stream that points to the contents of a zip file.
        @return true on success; false otherwise.
    */
    public static boolean addCatalog(String catalogName, InputStream is) {
        // NOTE: this method used by PulpCorePlayer via reflection 
        
        List catalogs = getContextCatalogs();

        if (is == null || catalogs == null) {
            return false;
        }

        AssetCatalog catalog = new AssetCatalog(catalogName);
        
        // Read zip file
        try {
            ZipInputStream in = new ZipInputStream(is);
            
            while (true) {
                ZipEntry entry = in.getNextEntry();
                if (entry == null) {
                    break;
                }
                
                int size = (int)entry.getSize();
                String entryName = entry.getName();
                byte[] entryData;
                if (size != -1) {
                    entryData = new byte[size];
                    int bytesToRead = size;
                    while (bytesToRead > 0) {
                        int bytesRead = in.read(entryData, size - bytesToRead, bytesToRead);
                        if (bytesRead == -1) {
                            if (Build.DEBUG) {
                                CoreSystem.print("Couldn't add asset (EOF reached): " + entryName);
                            }
                            in.close();
                            return false;
                        }
                        else {
                            bytesToRead -= bytesRead;
                        }
                    }
                }                                 
                else {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    while (true) {
                        int bytesRead = in.read(buffer);
                        if (bytesRead == -1) {
                            entryData = out.toByteArray();
                            break;
                        }
                        out.write(buffer, 0, bytesRead);
                    }
                }
                catalog.put(entryName, entryData);
            }
            in.close();
        }
        catch (IOException ex) {
            if (Build.DEBUG) CoreSystem.print("Couldn't add asset catalog: " + catalogName, ex);
            return false;
        }
        
        if (catalog.size() == 0) {
            if (Build.DEBUG) CoreSystem.print("Warning: no assets found: " + catalogName);
        }
        
        synchronized (LOCK) {
            // Remove old catalog of the same name
            removeCatalog(catalogName);

            catalogs.add(catalog);
        }
        return true;        
    }
    
    /**
        Gets an iterator of catalog names (zip files) stored in memory.
    */
    // Iterator<String>
    public static Iterator getCatalogs() {
        // NOTE: this method used by PulpCorePlayer via reflection
        List catalogs = getContextCatalogs();
        if (catalogs == null) {
            return null;
        }
        final List catalogsCopy = new ArrayList(catalogs);
        return new Iterator() {

            int index = 0;

            public boolean hasNext() {
                return index < catalogsCopy.size();
            }

            public Object next() {
                return ((AssetCatalog)catalogsCopy.get(index++)).getName();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
        Checks if the specified catalog name (zip file) is stored in memory.
    */
    public static boolean containsCatalog(String catalogName) {
        synchronized (LOCK) {
            List catalogs = getContextCatalogs();
            if (catalogName != null && catalogs != null) {
                for (int i = 0; i < catalogs.size(); i++) {
                    if (catalogName.equals(((AssetCatalog)catalogs.get(i)).getName())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    
    /**
        Removes all assets downloaded from the specified catalog (zip file).
    */
    public static void removeCatalog(String catalogName) {
        synchronized (LOCK) {
            // Remove from context's catalog list
            List catalogs = getContextCatalogs();
            if (catalogName != null && catalogs != null) {
                for (int i = 0; i < catalogs.size(); i++) {
                    if (catalogName.equals(((AssetCatalog)catalogs.get(i)).getName())) {
                        catalogs.remove(i);
                        break;
                    }
                }
            }
        }
    }

    /**
        Gets an iterator of the names of all assets from all zip files stored in memory. 
        Does not include assets in the jar file.
    */
    // Iterator<String>
    public static Iterator getAssetNames() {
        synchronized (LOCK) {
            List catalogs = getContextCatalogs();
            if (catalogs != null) {
                List assetNames = new ArrayList();
                for (int i = 0; i < catalogs.size(); i++) {
                    assetNames.addAll(((AssetCatalog)catalogs.get(i)).getAssetNames());
                }

                return assetNames.iterator();
            }
            return null;
        }
    }
    
    /**
        Returns true if the specified asset in any zip file exists. Does not check the jar.
    */
    public static boolean containsAsset(String assetName) {
        if (assetName.startsWith("/")) {
            assetName = assetName.substring(1);
        }
        
        synchronized (LOCK) {
            List catalogs = getContextCatalogs();
            if (catalogs != null) {
                for (int i = 0; i < catalogs.size(); i++) {
                    if (((AssetCatalog)catalogs.get(i)).contains(assetName)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static byte[] getBytes(String assetName) {
        List catalogs = getContextCatalogs();
        if (catalogs != null) {
            synchronized (LOCK) {
                // Reverse iteration. If the assetName is in multiple catalogs, uses the most
                // recently added.
                for (int i = catalogs.size() - 1; i >= 0; i--) {
                    AssetCatalog catalog = (AssetCatalog)catalogs.get(i);
                    byte[] data = catalog.get(assetName);
                    if (data != null) {
                        return data;
                    }
                }
            }
        }
        return null;
    }
    
    /**
        Gets an asset as a {@link pulpcore.util.ByteArray}.
        <p>
        This method first checks the downloaded asset catalogs, then the jar file. For some 
        platforms (Applets), the originating server is also checked if the
        asset is not in the zip file(s) or the jar file.
        Returns null if the asset was not found.
        <p>
        The returned ByteArray's position is set to zero.
    */
    public static ByteArray get(String assetName) {
        
        if (assetName.startsWith("/")) {
            assetName = assetName.substring(1);
        }

        // Check loaded zip file(s)
        byte[] assetData = getBytes(assetName);
        if (assetData != null) {
            return new ByteArray(assetData);
        }
        
        // Check the jar file, then the server
        Class parentLoader = CoreSystem.getPlatform().getClass();
        InputStream in = parentLoader.getResourceAsStream("/" + assetName);
        
        if (in == null) {
            if (Build.DEBUG) CoreSystem.print("Asset not found: " + assetName);
            return null;
        }
        else {
            ByteArray byteArray = new ByteArray();
            try {
                byteArray.write(in);
                in.close();
                byteArray.reset();
                return byteArray;
            }
            catch (IOException ex) {
                if (Build.DEBUG) CoreSystem.print("Error reading asset: " + assetName);
                return null;
            }
        }
    }
    
    /**
        Gets an asset as an {@link java.io.InputStream}.
        <p>
        This method first checks the downloaded asset catalogs, then the jar file. For some 
        platforms (Applets), the originating server is also checked if the
        asset is not in the zip file(s) or the jar file.
        Returns null if the asset was not found.
    */
    public static InputStream getAsStream(String assetName) {
        
        if (assetName.startsWith("/")) {
            assetName = assetName.substring(1);
        }
        
        // Check loaded zip file(s)
        byte[] assetData = getBytes(assetName);
        if (assetData != null) {
            return new ByteArrayInputStream(assetData);
        }
        
        // Check the jar file, then the server
        Class parentLoader = CoreSystem.getPlatform().getClass();
        InputStream in = parentLoader.getResourceAsStream("/" + assetName);
        
        if (in == null) {
            if (Build.DEBUG) CoreSystem.print("Asset not found: " + assetName);
        }
        
        return in;
    }
}