package net.java.cargotracker.interfaces.handling.file;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import net.java.cargotracker.domain.model.cargo.TrackingId;
import net.java.cargotracker.domain.model.handling.HandlingEvent;
import net.java.cargotracker.domain.model.location.UnLocode;
import net.java.cargotracker.domain.model.voyage.VoyageNumber;
import net.java.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;

@Dependent
@Named("EventItemReader")
public class EventItemReader extends AbstractItemReader {

    private static final String UPLOAD_DIRECTORY = "upload_directory";
    private static final String ISO_8601_FORMAT = "yyyy-MM-dd HH:mm";
    private static final Logger logger = Logger.getLogger(
            EventItemReader.class.getName());
    @Inject
    private JobContext jobContext;
    private EventFilesCheckpoint checkpoint;
    private RandomAccessFile currentFile;

    @Override
    public void open(Serializable checkpoint) throws Exception {
        File uploadDirectory = new File(
                jobContext.getProperties().getProperty(UPLOAD_DIRECTORY));

        if (checkpoint == null) {
            this.checkpoint = new EventFilesCheckpoint();
            logger.log(Level.INFO, "Scanning upload directory: {0}", uploadDirectory);

            if (!uploadDirectory.exists()) {
                logger.log(Level.INFO, "Upload directory does not exist, creating it");
                uploadDirectory.mkdirs();
            } else {
                this.checkpoint.setFiles(Arrays.asList(uploadDirectory.listFiles()));
            }
        } else {
            logger.log(Level.INFO, "Starting from previous checkpoint");
            this.checkpoint = (EventFilesCheckpoint) checkpoint;
        }

        File file = this.checkpoint.currentFile();

        if (file == null) {
            logger.log(Level.INFO, "No files to process");
            currentFile = null;
        } else {
            currentFile = new RandomAccessFile(file, "r");
            logger.log(Level.INFO, "Processing file: {0}", file);
            currentFile.seek(this.checkpoint.getFilePointer());
        }
    }

    @Override
    public Object readItem() throws Exception {
        if (currentFile != null) {
            String line = currentFile.readLine();

            if (line != null) {
                this.checkpoint.setFilePointer(currentFile.getFilePointer());
                return parseLine(line);
            } else {
                logger.log(Level.INFO, "Finished processing file, deleting: {0}",
                        this.checkpoint.currentFile());
                currentFile.close();
                this.checkpoint.currentFile().delete();
                File nextFile = this.checkpoint.nextFile();

                if (nextFile == null) {
                    logger.log(Level.INFO, "No more files to process");
                    return null;
                } else {
                    currentFile = new RandomAccessFile(nextFile, "r");
                    logger.log(Level.INFO, "Processing file: {0}", nextFile);
                    return readItem();
                }
            }
        } else {
            return null;
        }
    }

    private Object parseLine(String line) throws EventLineParseException {
        String[] result = line.split(",");

        if (result.length != 5) {
            throw new EventLineParseException("Wrong number of data elements", line);
        }

        Date completionTime = null;

        try {
            completionTime = new SimpleDateFormat(ISO_8601_FORMAT).parse(result[0]);
        } catch (ParseException e) {
            throw new EventLineParseException("Cannot parse completion time", e, line);
        }

        TrackingId trackingId = null;

        try {
            trackingId = new TrackingId(result[1]);
        } catch (NullPointerException e) {
            throw new EventLineParseException("Cannot parse tracking ID", e, line);
        }

        VoyageNumber voyageNumber = null;

        try {
            if (!result[2].isEmpty()) {
                voyageNumber = new VoyageNumber(result[2]);
            }
        } catch (NullPointerException e) {
            throw new EventLineParseException("Cannot parse voyage number", e, line);
        }

        UnLocode unLocode = null;

        try {
            unLocode = new UnLocode(result[3]);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new EventLineParseException("Cannot parse UN location code", e, line);
        }

        HandlingEvent.Type eventType = null;

        try {
            eventType = HandlingEvent.Type.valueOf(result[4]);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new EventLineParseException("Cannot parse event type", e, line);
        }

        HandlingEventRegistrationAttempt attempt
                = new HandlingEventRegistrationAttempt(new Date(), completionTime,
                        trackingId, voyageNumber, eventType, unLocode);

        return attempt;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return this.checkpoint;
    }
}
