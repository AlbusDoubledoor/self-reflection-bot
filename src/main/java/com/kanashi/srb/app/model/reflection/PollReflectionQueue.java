package com.kanashi.srb.app.model.reflection;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

public class PollReflectionQueue implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private transient final int MAX_SIZE = 10000;
    private transient final long MAX_AGE_MILLIS = 7 * (24 * 60 * 60 * 1000L); // 7 days

    private final LinkedList<Reflection> requests = new LinkedList<>();

    public synchronized void add(Reflection req) {
        if (requests.size() >= MAX_SIZE) {
            requests.removeFirst();
        }
        requests.addLast(req);
    }

    public synchronized Reflection getNext() {
        return requests.pollFirst();
    }

    @Nullable
    public synchronized Reflection getById(String id) {
        for (Reflection reflection : requests) {
            if (reflection.getId().equals(id)) {
                return reflection;
            }
        }

        return null;
    }

    public synchronized void removeById(String id) {
        for (Reflection reflection : requests) {
            if (reflection.getId().equals(id)) {
                requests.remove(reflection);
                return;
            }
        }
    }

    public synchronized int size() {
        return requests.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Reflection reflection : requests) {
            sb.append('\n')
                    .append("id=")
                    .append(reflection.getId())
                    .append("; timestamp=")
                    .append(reflection.getTimestamp())
                    .append(' ')
                    .append(ReflectionWriter.print(reflection));
        }

        return sb.toString();
    }

    public synchronized void cleanupOld() {
        long now = System.currentTimeMillis();
        requests.removeIf(r -> (now - r.getTimestamp()) > MAX_AGE_MILLIS);
    }


    public synchronized void saveToFile(String filename) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(this);
        }
    }

    public static PollReflectionQueue loadFromFile(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            return (PollReflectionQueue) in.readObject();
        }
    }
}
