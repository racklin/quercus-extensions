/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.racklin.quercus.lib.bzip2;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.vfs.StreamImplOutputStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.InputStream;
import java.util.logging.Logger;

import org.apache.commons.compress.compressors.bzip2.*;

public class Bzip2Module extends AbstractQuercusModule {

    private static final Logger log = Logger.getLogger(Bzip2Module.class.getName());
    private static final L10N L = new L10N(Bzip2Module.class);
    private int _dbg;

    public String[] getLoadedExtensions() {
        return new String[]{"bzip2"};
    }

    /**
     * compresses data using BZIP2
     *
     * @param data
     * @param level (default is 4)
     * @return compressed string
     */
    public Value bzcompress(Env env,
            InputStream data,
            @Optional("4") int level) {
        TempBuffer tempBuf = TempBuffer.allocate();
        byte[] buffer = tempBuf.getBuffer();

        if (level < 1) {
            level = 1;
        } else if (level > 9) {
            level = 9;
        }

        BZip2CompressorOutputStream gos;

        try {

            TempStream out = new TempStream();
            StreamImplOutputStream os = new StreamImplOutputStream(out);
            gos = new BZip2CompressorOutputStream(os, level);
            int len = 0;

            while ((len = data.read(buffer, 0, buffer.length)) >= 0) {
                gos.write(buffer, 0, len);
            }

            gos.finish();
            gos.flush();
            gos.close();

            return env.createBinaryString(out.getHead());

        } catch (Exception e) {
            throw QuercusModuleException.create(e);
        } finally {
            TempBuffer.free(tempBuf);

        }

    }

    /**
     * @param is compressed using BZIP2 algorithm
     *
     * @return uncompressed string
     */
    public Value bzdecompress(Env env,
            InputStream is) {

        TempBuffer tempBuf = TempBuffer.allocate();
        byte[] buffer = tempBuf.getBuffer();

        BZip2CompressorInputStream in = null;

        try {
            // XXX: not working with inputstream is ?
            // in = new BZip2CompressorInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            StringValue sb = env.createLargeBinaryBuilder();

            int len = 0;
            while ((len = is.read(buffer, 0, buffer.length)) >= 0) {
                baos.write(buffer, 0, len);
            }

            in = new BZip2CompressorInputStream(new ByteArrayInputStream(baos.toByteArray()));

            baos.close();

            len = 0;
            while ((len = in.read(buffer, 0, buffer.length)) >= 0) {
                sb.append(buffer, 0, len);
            }

            return sb;
        } catch (OutOfMemoryError e) {
            env.warning(e);
            return BooleanValue.FALSE;
        } catch (Exception e) {
            env.warning(e);
            return BooleanValue.FALSE;
        } finally {
            TempBuffer.free(tempBuf);

        }
    }
}
