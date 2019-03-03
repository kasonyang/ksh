package site.kason.ksh;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GitRepository {

    private final Repository repository;
    private final Git git;
    private String path;

    public GitRepository(String path) throws IOException {
        this.path = path;
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(new File(path))
                .readEnvironment()
                .findGitDir()
                .build();
        git = new Git(repository);
    }

    public void checkout(String branch) throws GitAPIException {
        git.checkout().setName(branch).call();
    }

    public List<String> listRemoteBranches() throws GitAPIException {
        return listBranches(ListBranchCommand.ListMode.REMOTE);
    }

    public List<String> listBranches() throws GitAPIException {
        return listBranches(ListBranchCommand.ListMode.ALL);
    }

    private List<String> listBranches(ListBranchCommand.ListMode mode) throws GitAPIException {
        Git git = new Git(repository);
        List<Ref> branches = git.branchList().setListMode(mode).call();
        List<String> results = new ArrayList<>(branches.size());
        for(Ref b:branches) {
            results.add(b.getName());
        }
        return results;
    }

}
