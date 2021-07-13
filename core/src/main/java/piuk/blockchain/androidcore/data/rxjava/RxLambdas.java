package piuk.blockchain.androidcore.data.rxjava;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;

public final class RxLambdas {

    // For collapsing into Lambdas
    public interface ObservableRequest<T> {
        Observable<T> apply();
    }

    // For collapsing into Lambdas
    public interface SingleRequest<T> {
        Single<T> apply();
    }

    // For collapsing into Lambdas
    public interface CompletableRequest {
        Completable apply();
    }

    public abstract static class ObservableFunction<T> implements Function<Void, Observable<T>> {
        public abstract Observable<T> apply(Void empty);
    }

    public abstract static class SingleFunction<T> implements Function<Void, Single<T>> {
        public abstract Single<T> apply(Void empty);
    }

    public abstract static class CompletableFunction implements Function<Void, Completable> {
        public abstract Completable apply(Void empty);
    }

}
