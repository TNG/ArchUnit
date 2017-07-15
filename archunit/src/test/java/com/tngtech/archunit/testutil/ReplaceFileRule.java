package com.tngtech.archunit.testutil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.junit.rules.ExternalResource;

import static com.google.common.base.Preconditions.checkState;

public class ReplaceFileRule extends ExternalResource {
    private final File tempDir = TestUtils.newTemporaryFolder();

    private final List<FileAction> fileActions = new ArrayList<>();
    private final List<MoveAction> moveActions = new ArrayList<>();
    private final Set<File> replacedFiles = new HashSet<>();

    public void replace(File target, String content, Charset charset) {
        if (target.exists()) {
            addMoveAction(new MoveAction(target, new File(tempDir, target.getName())).execute());
        }
        replacedFiles.add(target);
        makePath(target);
        write(target, content, charset);
    }

    private void makePath(File target) {
        LinkedList<FileAction> mkdirs = new LinkedList<>();
        while (!target.getParentFile().exists()) {
            mkdirs.add(0, new MkDirAction(target.getParentFile()));
            target = target.getParentFile();
        }
        for (FileAction mkdir : mkdirs) {
            mkdir.execute();
        }
        fileActions.addAll(mkdirs);
    }

    public void appendLine(File file, String line, Charset charset) {
        if (replacedFiles.contains(file)) {
            append(file, line, charset);
        } else {
            replace(file, line, charset);
        }
    }

    private void write(File target, String content, Charset charset) {
        fileActions.add(new CreateFileAction(target, content, charset).execute());
    }

    private void append(File file, String line, Charset charset) {
        try {
            com.google.common.io.Files.append(System.lineSeparator() + line, file, charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addMoveAction(MoveAction moveAction) {
        fileActions.add(moveAction);
        moveActions.add(moveAction);
    }

    @Override
    protected void after() {
        for (FileAction action : Lists.reverse(fileActions)) {
            action.revert();
        }
    }

    private interface FileAction {
        FileAction execute();

        void revert();
    }

    private static class MoveAction implements FileAction {
        private final File from;
        private final File to;

        private MoveAction(File from, File to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public MoveAction execute() {
            move(from, to);
            return this;
        }

        @Override
        public void revert() {
            move(to, from);
        }

        private void move(File origin, File target) {
            try {
                java.nio.file.Files.move(origin.toPath(), target.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class MkDirAction implements FileAction {
        private final File dir;

        private MkDirAction(File dir) {
            this.dir = dir;
        }

        @Override
        public FileAction execute() {
            checkState(dir.mkdir());
            return this;
        }

        @Override
        public void revert() {
            checkState(dir.delete());
        }
    }

    private static class CreateFileAction implements FileAction {
        private final File target;
        private final String content;
        private final Charset charset;

        private CreateFileAction(File target, String content, Charset charset) {
            this.target = target;
            this.content = content;
            this.charset = charset;
        }

        @Override
        public FileAction execute() {
            try {
                com.google.common.io.Files.write(content, target, charset);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        @Override
        public void revert() {
            checkState(target.delete());
        }
    }
}
