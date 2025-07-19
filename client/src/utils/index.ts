export const getStatusColor = (status: string): string => {
  switch (status) {
    case "online":
      return "text-green-500";
    case "offline":
      return "text-gray-400";
    default:
      return "text-gray-400";
  }
};

export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return "0 Bytes";
  const k = 1024;
  const sizes = ["Bytes", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
};

export const calculateAge = (birthDate: string): number => {
  if (!birthDate || birthDate.trim() === '') {
    return 0;
  }
  
  const today = new Date();
  const birth = new Date(birthDate.split("/").reverse().join("-"));
  
  // Verificar se a data é válida
  if (isNaN(birth.getTime())) {
    return 0;
  }
  
  let age = today.getFullYear() - birth.getFullYear();
  const monthDiff = today.getMonth() - birth.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--;
  }
  return age;
};

export const getCurrentTimestamp = (): string => {
  return new Date().toLocaleTimeString("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
  });
};
