package test.site.kason.ksh;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import site.kason.ksh.GitRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GitRepositoryTest {

    @Test
    public void testRemoteBranches() throws IOException, GitAPIException {
        String path = new File(".git").getAbsolutePath();
        GitRepository git = new GitRepository(path);
        List<String> bs = git.listBranches();
        System.out.println(bs);
    }

}
