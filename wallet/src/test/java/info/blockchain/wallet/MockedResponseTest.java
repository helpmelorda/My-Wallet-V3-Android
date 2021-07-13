package info.blockchain.wallet;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import info.blockchain.wallet.api.Environment;
import io.reactivex.rxjava3.internal.schedulers.TrampolineScheduler;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public abstract class MockedResponseTest {

    protected MockInterceptor mockInterceptor;

    @Before
    public void initBlockchainFramework() {
        mockInterceptor = new MockInterceptor();
        BlockchainFramework.init(frameworkInterface(newOkHttpClient()));
    }

    private FrameworkInterface frameworkInterface(final OkHttpClient okHttpClient) {
        return new FrameworkInterface() {
            @Override
            public Retrofit getRetrofitApiInstance() {
                return getRetrofit("https://api.staging.blockchain.info/", okHttpClient);
            }

            @Override
            public Environment getEnvironment() {
                return Environment.STAGING;
            }

            @NotNull
            @Override
            public String getApiCode() { return "API_CODE"; }

            @Override
            public String getDevice() {
                return "UnitTest";
            }

            @Override
            public String getAppVersion() {
                return null;
            }
        };
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
        BlockchainFramework.init(null);
    }

    OkHttpClient newOkHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(mockInterceptor)//Mock responses
                .addInterceptor(new ApiInterceptor())//Extensive logging
                .build();
    }

    Retrofit getRetrofit(String url, OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    protected String loadResourceContent(String resourceFile) {
        try {
            URI uri = getClass().getClassLoader().getResource(resourceFile).toURI();
            return new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}