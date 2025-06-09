import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Gerar hash para senha "admin123"
        String password = "admin123";
        String hash = encoder.encode(password);
        
        System.out.println("Senha: " + password);
        System.out.println("Hash: " + hash);
        
        // Verificar se bate com o hash existente
        String existingHash = "$2a$10$L9e87GS/XYAGwh8Jaj8Gn.MSF7ocNYUqCnjdFO47Stu80h.7WkOL.";
        
        // Testar senhas comuns
        String[] commonPasswords = {"password", "123456", "admin", "senha123", "test", "admin123"};
        
        for (String testPassword : commonPasswords) {
            if (encoder.matches(testPassword, existingHash)) {
                System.out.println("ENCONTROU! A senha é: " + testPassword);
                return;
            }
        }
        
        System.out.println("Senha não encontrada nas tentativas comuns");
    }
}