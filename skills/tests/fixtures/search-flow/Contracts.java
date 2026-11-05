package lesson;

interface ResultCallback<T> {
    void success(T value);
    void failure(Exception error);
}

interface SearchApi {
    String fetch(String query) throws Exception;
}

interface SearchView {
    void showLoading();
    void showResult(String value);
    void showError(String message);
}
