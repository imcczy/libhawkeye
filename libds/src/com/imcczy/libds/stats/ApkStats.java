package com.imcczy.libds.stats;

import com.imcczy.libds.manifest.ProcessManifest;
import com.imcczy.libds.signature.Node;
import com.imcczy.libds.utils.Dependence;
import com.imcczy.libds.utils.Edge;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.imcczy.libds.manifest.AndroidManifestConstants.MAGIC;


/**
 * Created by imcczy on 2017/3/26.
 */
public class ApkStats implements Serializable {
    private static final long serialVersionUID = -3424680417404895550L;
    public String pcakageName;
    public String versionCode;
    public String versionName;
    public String mainActivityName;
    public Node node;
    public TreeMap<Edge, Dependence> dependenceTreeMap;


    public ApkStats(Path path) throws IOException {
        parse(path);
    }

    public void parse(Path path) throws IOException {
        /*
        List<Path> list = null;
        try (FileSystem fileSystem = FileSystems.newFileSystem(path,null)){
            list = Files.list(fileSystem.getPath("/META-INF/"))
                    .filter(zipPath -> zipPath.toString().endsWith(".RSA") || zipPath.toString().endsWith(".DSA"))
                    .collect(Collectors.toList());
        }catch (IOException e){
            e.printStackTrace();
        }
        */
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            ZipEntry zipEntry = zipFile.getEntry("AndroidManifest.xml");
            if (zipEntry == null) {
                throw new IOException("No AndroidManifest.xml");
            }

            InputStream inputStream = zipFile.getInputStream(zipEntry);
            ReadableByteChannel channel = Channels.newChannel(inputStream);
            ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
            }
            buffer.flip();
            int magic = buffer.getInt(); // Magic
            int fileSize = buffer.getInt(); // File Size
            buffer.rewind();
            assert magic == MAGIC;

            buffer = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN).put(buffer);
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
            }
            buffer.flip();
            ProcessManifest pm = new ProcessManifest();
            pm.parse(buffer);

            this.pcakageName = pm.getPackageName();
            this.versionCode = pm.getVersionCode();
            this.versionName = pm.getVersionName();
            this.mainActivityName = pm.getActivityName();
            /*
            for (Path cert:list){
                ZipEntry zipEntry1 = zipFile.getEntry(cert.toString().substring(1));
                if (zipEntry1 == null) {
                    throw new IOException("No RSA || DSA");
                }
                CertificateParser parser = new CertificateParser(Utils.toByteArray(zipFile.getInputStream(zipEntry1)));
                parser.parse();
                this.certificateMetaList = parser.getCertificateMetas();
            }
            */

        }
    }
}
