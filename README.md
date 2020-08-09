![Maven Central](https://img.shields.io/maven-central/v/site.kason/ksh.svg)

kalang script helpers

# Installation
    
### Gradle
    
    compile 'site.kason:ksh:2.1.0'
    
# Usages

## Shell


    import site.kason.ksh.Shell;
    val shell = new Shell();
    shell.cd("/tmp/");
    assert 0 == shell.exec("ls");
    
## SshClient

    val sshClient = SshClient();
    ssh.addConsoleHostKeyVerifier();
    ssh.connect("127.0.0.1", 22);
    ssh.authPublicKey(YOUR_USER_NANE);
    val result = ssh.exec("ls");
    assert result.getExitStatus() == 0;
    println(result.getOutput());
    
### ShellOptions

    val options =  ShellOptions.newBuilder("ShellOptionTest")
        .header("ShellOption Test")
        .footer("Good luck!")
        .option("h","help",false,"print help message")
        .option("p","path",true,"specify path")
        .args(["name"],["nickname"])
        .build(args);
    if (options.hasError()) {
        println(options.getError());
        return;
    }
    if (options.hasOption("help")) {
        println(options.getUsage());
    }
    val name = options.getArg(0);
    val nickname = options.getArg(1, name);
    println(name);
    println(nickname);
    
    