package org.redukti.jfotoptix.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Helper {

    public static Path getOutputPath(Args arguments) {
        if (arguments.outputFile == null) {
            throw new IllegalArgumentException("Output file name not specified");
        }
        if (arguments.outdir != null) {
            return Paths.get(arguments.outdir, arguments.outputFile);
        }
        Path path = new File(arguments.specfile).toPath().toAbsolutePath();
        return Paths.get(path.getParent().toString(), arguments.outputFile);
    }

    public static Path getOutputPath(Args arguments, String extension) {
        if (arguments.outputFile == null) {
            throw new IllegalArgumentException("Output file name not specified");
        }
        if (arguments.outdir != null) {
            return Paths.get(arguments.outdir, arguments.outputFile + extension);
        }
        Path path = new File(arguments.specfile).toPath().toAbsolutePath();
        return Paths.get(path.getParent().toString(), arguments.outputFile + extension);
    }

    public static Path getOutputPath(String specfile, String outputFile, String outdir) {
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file name not specified");
        }
        if (outdir != null) {
            return Paths.get(outdir, outputFile);
        }
        Path path = new File(specfile).toPath().toAbsolutePath();
        return Paths.get(path.getParent().toString(), outputFile);
    }

    public static Path getOutputPathChangeExt(String specfile, String extension) {
        Path path = new File(specfile).toPath().toAbsolutePath();
        String fileName = path.getFileName().toString();
        // Remove extension
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            fileName = fileName.substring(0, dotIndex);
        }
        return Paths.get(path.getParent().toString(), fileName + extension);
    }

    public static void createOutputFile(Path outpath, String string) throws IOException {
        try {
            Files.write(outpath, string.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (Exception e) {
            System.err.println("Failed to create file " + outpath);
            e.printStackTrace();
        }
    }
}
