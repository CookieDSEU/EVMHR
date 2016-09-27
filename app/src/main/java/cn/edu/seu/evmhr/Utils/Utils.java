package cn.edu.seu.evmhr.Utils;
import android.hardware.Camera;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Cookie_D on 2016/9/26.
 */

public class Utils {
    private Utils() {
    }
    public static List<Camera.Size> getResolutionList(Camera camera)
    {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        return previewSizes;
    }

    public static class ResolutionComparator implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if(lhs.height!=rhs.height)
                return lhs.height-rhs.height;
            else
                return lhs.width-rhs.width;
        }

    }
    public static boolean isExitsSdcard() {
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return true;
        else
            return false;
    }
}
