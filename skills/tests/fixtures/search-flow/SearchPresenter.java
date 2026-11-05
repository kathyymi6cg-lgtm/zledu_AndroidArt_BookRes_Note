package lesson;

public final class SearchPresenter {
    private final SearchRepository repository;
    private final SearchView view;
    private int generation;

    public SearchPresenter(SearchRepository repository, SearchView view) {
        this.repository = repository;
        this.view = view;
    }

    // Called on the Android main thread by the search button listener.
    public void search(String raw) {
        final int request = ++generation;
        final String query = raw == null ? "" : raw.trim();
        if (query.isEmpty()) {
            view.showError("请输入关键词");
            return;
        }
        view.showLoading();
        repository.find(query, new ResultCallback<String>() {
            @Override public void success(String value) {
                if (request != generation) return;
                view.showResult(value);
            }
            @Override public void failure(Exception error) {
                if (request != generation) return;
                view.showError(error.getMessage());
            }
        });
    }
}
