package info.blockchain.wallet;

import org.jetbrains.annotations.NotNull;
import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.api.PersistentUrls;
import io.reactivex.rxjava3.internal.schedulers.TrampolineScheduler;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public abstract class BaseIntegTest {

    @BeforeClass
    public static void init() {
        //Initialize framework
        BlockchainFramework.init(new FrameworkInterface() {
            @Override
            public Retrofit getRetrofitApiInstance() {
                return getRetrofit(PersistentUrls.API_URL, getOkHttpClient());
            }

            @Override
            public Environment getEnvironment() {
                return Environment.PRODUCTION;
            }

            @NotNull
            @Override
            public String getApiCode() {
                return "Android-Integration-test";
            }

            @Override
            public String getDevice() {
                return "Android Integration test";
            }

            @Override
            public String getAppVersion() {
                return "1.0";
            }
        });
    }

    @Before
    public void setupRxCalls() {
        RxJavaPlugins.reset();


        RxJavaPlugins.setIoSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
        RxJavaPlugins.setComputationSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
        RxJavaPlugins.setNewThreadSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
    }

    @After
    public void tearDownRxCalls() {
        RxJavaPlugins.reset();
    }

    public static OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new ApiInterceptor())//Extensive logging
                .build();
    }

    public static Retrofit getRetrofit(String url, OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }
}