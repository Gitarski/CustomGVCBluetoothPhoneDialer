package customphonedialer.abror96.customphonedialer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telecom.Call;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import kotlin.collections.CollectionsKt;

import static customphonedialer.abror96.customphonedialer.Constants.asString;

public class CallActivity extends AppCompatActivity {

    @BindView(R.id.answer)
    Button answer;
    @BindView(R.id.hangup)
    Button hangup;
    @BindView(R.id.callInfo)
    TextView callInfo;


    private CompositeDisposable disposables;
    private String number;
    private OngoingCall ongoingCall;

    private static final String gvcPackage = "com.judlis.googlevoicecallhandler4joying";
    private static final String gvcClass = "com.judlis.googlevoicecallhandler4joying.CallHandler";
    private static final String bluetoothPackage = "com.syu.bt";
    private static final String bluetoothClass1 = "com.syu.app.PhoneActivity";
    private static final String bluetoothClass2 = "com.syu.bt.PhoneActivity";
    private static boolean isBluetoothCallable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        ButterKnife.bind(this);

        ongoingCall = new OngoingCall();
        disposables = new CompositeDisposable();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        number = Objects.requireNonNull(getIntent().getData()).getSchemeSpecificPart();
    }

    @OnClick(R.id.answer)
    public void onAnswerClicked() {
        ongoingCall.answer();
    }

    @OnClick(R.id.hangup)
    public void onHangupClicked() {
        ongoingCall.hangup();
    }



    @Override
    protected void onStart() {
        super.onStart();

        assert updateUi(-1) != null;
        disposables.add(
                OngoingCall.state
                        .subscribe(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) throws Exception {
                                updateUi(integer);
                            }
                        }));

        disposables.add(
                OngoingCall.state
                        .filter(new Predicate<Integer>() {
                            @Override
                            public boolean test(Integer integer) throws Exception {
                                return integer == Call.STATE_DISCONNECTED;
                            }
                        })
                        .delay(1, TimeUnit.SECONDS)
                        .firstElement()
                        .subscribe(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) throws Exception {
                                finish();
                            }
                        }));

    }

    @SuppressLint("SetTextI18n")
    private Consumer<? super Integer> updateUi(Integer state) {

        callInfo.setText(asString(state) + "\n" + number);

        if (state != Call.STATE_RINGING) {
            answer.setVisibility(View.GONE);
        } else answer.setVisibility(View.VISIBLE);

        if (CollectionsKt.listOf(new Integer[]{
                Call.STATE_DIALING,
                Call.STATE_RINGING,
                Call.STATE_ACTIVE}).contains(state)) {
            hangup.setVisibility(View.VISIBLE);
        } else
            hangup.setVisibility(View.GONE);

        return null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    public static void start(Context context, Call call) {
        /* Old code
        Intent intent = new Intent(context, CallActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(call.getDetails().getHandle());
        context.startActivity(intent);
       */

        try {

            isBluetoothCallable = false;

            Intent gvcIntent = new Intent(Intent.ACTION_CALL).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            gvcIntent.setClassName(gvcPackage, gvcClass);
            gvcIntent.setData(Uri.parse("tel:" + call.getDetails().getHandle().getSchemeSpecificPart()));

            if (isCallable(context, gvcIntent)) {

                Intent bluetoothIntent1 = new Intent(Intent.ACTION_CALL).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                bluetoothIntent1.setClassName(bluetoothPackage, bluetoothClass1);
                bluetoothIntent1.setData(Uri.parse("tel:" + call.getDetails().getHandle().getSchemeSpecificPart()));

                if (isCallable(context, bluetoothIntent1)) {
                    isBluetoothCallable = true;
                } else {

                    Intent bluetoothIntent2 = new Intent(Intent.ACTION_CALL).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    bluetoothIntent2.setClassName(bluetoothPackage, bluetoothClass2);
                    bluetoothIntent2.setData(Uri.parse("tel:" + call.getDetails().getHandle().getSchemeSpecificPart()));

                    if (isCallable(context, bluetoothIntent2)) {
                        isBluetoothCallable = true;
                    }
                }

                if (isBluetoothCallable) {
                    context.startActivity(gvcIntent);
                } else {
                    Toast.makeText(context, R.string.bt_app_notfound, Toast.LENGTH_LONG).show();
//                    throw new RuntimeException(String.valueOf(R.string.bt_app_notfound));
                }
            } else {
                Toast.makeText(context, R.string.gvc_app_notfound, Toast.LENGTH_LONG).show();
//                throw new RuntimeException(String.valueOf(R.string.gvc_app_notfound));
            }
        } catch (Exception e) {
            Toast.makeText(context, R.string.unexpected_error_failed, Toast.LENGTH_LONG).show();
            throw e;
//            throw new RuntimeException(String.valueOf(R.string.unexpected_error_failed));
        }
    }

    private static boolean isCallable(final Context context, Intent intent) {
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL);
        return list.size() > 0;
    }
}
