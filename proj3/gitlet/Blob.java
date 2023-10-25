package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {

    /** The name of the file. */
    private String _blobName;

    /** The content of the file. */
    private String _blobContent;

    /** The ID of this blob. */
    private String _blobID;

    public Blob(File file) {
        _blobContent = Utils.readContentsAsString(file);
        _blobName = file.getName();
        _blobID = Utils.sha1(Utils.serialize(this));
    }

    public static String getName(Blob blob) {
        return blob._blobName;
    }

    public static String getID(Blob blob) {
        return blob._blobID;
    }

    public static String getContent(Blob blob) {
        return blob._blobContent;
    }

    @Override
    public boolean equals(Object newBlob) {
        if (newBlob == null) {
            return false;
        }
        if (newBlob.getClass() != this.getClass()) {
            return false;
        }
        Blob nowBlob = (Blob) newBlob;
        return this._blobContent.equals(nowBlob._blobContent);
    }

    public int hashCode() {
        return 0;
    }
}
