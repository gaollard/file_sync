public class AppLogFragment {
    private static final String TAG = "AppLogFragment";

    private TextView appLogTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_log, container, false);
    }
}
