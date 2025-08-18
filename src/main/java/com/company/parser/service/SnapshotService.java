package com.company.parser.service;

import com.company.parser.core.Snapshot;
import com.company.parser.core.SnapshotStore;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.Files.copy;

@Service
public class SnapshotService {
    public Snapshot loadPrev() throws Exception {
        return new SnapshotStore(Path.of("snapshot_prev.csv")).load();
    }
    public void saveCur(Snapshot cur) throws Exception {
        new SnapshotStore(Path.of("snapshot_cur.csv")).save(cur);
    }
    public void advanceBaseline() throws Exception {
        copy(Path.of("snapshot_cur.csv"), Path.of("snapshot_prev.csv"), REPLACE_EXISTING);
    }
}