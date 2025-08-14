import React, { useState, useEffect, useCallback } from "react";
import "./styles.css";
import { Plus, Edit3, Trash2, MoreVertical, HelpCircle } from "lucide-react";
import {
  Button,
  Input,
  message,
  Dropdown,
  Modal,
  Table,
  Switch,
  Card,
  Typography,
  Spin,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import { FAQModal } from "../FAQModal";
import { faqService, type FAQ, type FAQStats } from "../../services/faqService";

// Re-export FAQ type for components that import from this file
export type { FAQ } from "../../services/faqService";
import { useAuthStore } from "../../store/useAuthStore";

const { Search: AntSearch } = Input;
const { Title } = Typography;

interface FAQManagementProps {}

export const FAQManagement: React.FC<FAQManagementProps> = () => {
  const { user } = useAuthStore();
  const [faqs, setFaqs] = useState<FAQ[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedFAQ, setSelectedFAQ] = useState<FAQ | null>(null);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [stats, setStats] = useState<FAQStats | null>(null);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  // Load FAQs from API
  const loadFAQs = useCallback(
    async (resetPagination = false) => {
      setLoading(true);
      try {
        const currentPage = resetPagination ? 1 : pagination.current;

        const response = await faqService.searchFAQs({
          searchTerm: searchTerm || undefined,
          page: currentPage - 1, // Backend is 0-indexed
          size: pagination.pageSize,
          sortBy: "createdAt",
          sortDir: "desc",
        });

        setFaqs(response.content);
        setPagination((prev) => ({
          ...prev,
          current: resetPagination ? 1 : currentPage,
          total: response.totalElements || 0,
        }));
      } catch (error) {
        console.error("Erro ao carregar FAQs:", error);
        message.error("Erro ao carregar FAQs");
      } finally {
        setLoading(false);
      }
    },
    [searchTerm, pagination.current, pagination.pageSize]
  );

  // Load FAQ statistics
  const loadStats = useCallback(async () => {
    try {
      const statsData = await faqService.getFAQStats();
      setStats(statsData);
    } catch (error) {
      console.error("Erro ao carregar estatísticas:", error);
      // Don't show error message for stats as it's not critical
    }
  }, []);

  // Initial load
  useEffect(() => {
    const initialLoad = async () => {
      setLoading(true);
      try {
        const response = await faqService.searchFAQs({
          page: 0,
          size: pagination.pageSize,
          sortBy: "createdAt",
          sortDir: "desc",
        });

        setFaqs(response.content);
        setPagination((prev) => ({
          ...prev,
          current: 1,
          total: response.totalElements || 0,
        }));

        // Load stats separately
        try {
          const statsData = await faqService.getFAQStats();
          setStats(statsData);
        } catch (error) {
          console.error("Erro ao carregar estatísticas:", error);
        }
      } catch (error) {
        console.error("Erro ao carregar FAQs:", error);
        message.error("Erro ao carregar FAQs");
      } finally {
        setLoading(false);
      }
    };

    initialLoad();
  }, []); // Empty dependency array for initial load only

  // Reload when search term changes with debounce
  useEffect(() => {
    if (searchTerm !== "") {
      const timeoutId = setTimeout(() => {
        loadFAQs(true); // Reset to first page when searching
      }, 500);

      return () => clearTimeout(timeoutId);
    }
  }, [searchTerm]);

  // Reload when pagination changes
  useEffect(() => {
    if (pagination.current > 1) {
      loadFAQs(false);
    }
  }, [pagination.current, pagination.pageSize]);

  const handleEdit = (faq: FAQ) => {
    setSelectedFAQ(faq);
    setIsModalVisible(true);
  };

  const handleDelete = async (id: string) => {
    try {
      await faqService.deleteFAQ(id);
      message.success("FAQ excluída com sucesso!");
      loadFAQs(); // Reload list
      loadStats(); // Reload stats
    } catch (error) {
      console.error("Erro ao excluir FAQ:", error);
      message.error("Erro ao excluir FAQ");
    }
  };

  const handleToggleStatus = async (id: string, isActive: boolean) => {
    try {
      await faqService.toggleFAQStatus(id, isActive);
      message.success(
        `FAQ ${isActive ? "ativada" : "desativada"} com sucesso!`
      );
      loadFAQs(); // Reload list
      loadStats(); // Reload stats
    } catch (error) {
      console.error("Erro ao alterar status da FAQ:", error);
      message.error("Erro ao alterar status da FAQ");
    }
  };

  const handleSaveFAQ = async (faqData: Partial<FAQ>) => {
    try {
      if (!user?.companyId) {
        message.error("Erro: Usuário não possui empresa associada");
        return;
      }

      if (selectedFAQ) {
        // Update existing FAQ
        await faqService.updateFAQ(selectedFAQ.id, {
          question: faqData.question,
          answer: faqData.answer,
          keywords: faqData.keywords,
          triggers: faqData.triggers,
          isActive: faqData.isActive,
        });
        message.success("FAQ atualizada com sucesso!");
      } else {
        // Create new FAQ
        await faqService.createFAQ({
          companyId: user.companyId,
          question: faqData.question || "",
          answer: faqData.answer || "",
          keywords: faqData.keywords || [],
          triggers: faqData.triggers || [],
          isActive: true,
        });
        message.success("FAQ criada com sucesso!");
      }

      loadFAQs(); // Reload list
      loadStats(); // Reload stats
    } catch (error) {
      console.error("Erro ao salvar FAQ:", error);
      message.error("Erro ao salvar FAQ");
    }

    setIsModalVisible(false);
    setSelectedFAQ(null);
  };

  const handleSearch = (value: string) => {
    setSearchTerm(value);
  };

  const handlePageChange = (page: number, pageSize?: number) => {
    setPagination((prev) => ({
      ...prev,
      current: page,
      pageSize: pageSize || prev.pageSize,
    }));
  };

  const columns: ColumnsType<FAQ> = [
    {
      title: "Pergunta",
      dataIndex: "question",
      key: "question",
      ellipsis: true,
      render: (text: string) => (
        <div className="font-medium text-gray-800">{text}</div>
      ),
    },
    {
      title: "Status",
      dataIndex: "isActive",
      key: "isActive",
      render: (isActive: boolean, record: FAQ) => (
        <Switch
          checked={isActive}
          onChange={(checked) => handleToggleStatus(record.id, checked)}
          size="small"
        />
      ),
      width: 100,
    },
    {
      title: "Ações",
      key: "actions",
      render: (_, record: FAQ) => (
        <Dropdown
          menu={{
            items: [
              {
                key: "edit",
                label: "Editar",
                icon: <Edit3 className="w-4 h-4" />,
                onClick: () => handleEdit(record),
              },
              {
                key: "delete",
                label: "Excluir",
                icon: <Trash2 className="w-4 h-4" />,
                danger: true,
                onClick: () => {
                  Modal.confirm({
                    title: "Confirmar exclusão",
                    content: "Tem certeza que deseja excluir esta FAQ?",
                    okText: "Excluir",
                    okType: "danger",
                    cancelText: "Cancelar",
                    onOk: () => handleDelete(record.id),
                  });
                },
              },
            ],
          }}
          trigger={["click"]}
        >
          <Button
            type="text"
            icon={<MoreVertical className="w-4 h-4" />}
            size="small"
          />
        </Dropdown>
      ),
      width: 80,
    },
  ];

  return (
    <div className="max-w-7xl mx-auto p-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 bg-red-100 rounded-xl flex items-center justify-center">
            <HelpCircle className="w-6 h-6 text-red-600" />
          </div>
          <div>
            <Title level={2} className="!mb-0">
              Gerenciamento de FAQ
            </Title>
            <p className="text-gray-600 mt-1">
              Configure perguntas e respostas para a IA utilizar
            </p>
          </div>
        </div>
        <Button
          type="primary"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => setIsModalVisible(true)}
          size="large"
          className="bg-red-500 hover:bg-red-600 border-red-500 hover:border-red-600"
        >
          Nova FAQ
        </Button>
      </div>

      {/* Info Card */}
      <Card className="mb-6">
        <div className="flex items-start gap-4">
          <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0">
            <HelpCircle className="w-4 h-4 text-blue-600" />
          </div>
          <div>
            <h3 className="font-medium text-gray-800 mb-2">
              Como as FAQs funcionam com a IA
            </h3>
            <p className="text-sm text-gray-600 leading-relaxed">
              A IA utiliza as FAQs cadastradas para gerar respostas automáticas
              em rascunhos. Quando um cliente faz uma pergunta similar às
              registradas aqui, o sistema busca a FAQ mais relevante e usa como
              base para criar uma resposta personalizada.
            </p>
          </div>
        </div>
      </Card>

      {/* Filters */}
      <Card className="mb-6">
        <AntSearch
          placeholder="Buscar por pergunta, resposta ou palavra-chave..."
          allowClear
          enterButton
          onSearch={handleSearch}
          onChange={(e) => e.target.value === "" && setSearchTerm("")}
          size="large"
        />
      </Card>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        <Card size="small">
          <div className="text-center">
            {stats ? (
              <>
                <div className="text-2xl font-bold text-gray-800">
                  {stats.totalFAQs}
                </div>
                <div className="text-sm text-gray-600">Total FAQs</div>
              </>
            ) : (
              <Spin size="small" />
            )}
          </div>
        </Card>
        <Card size="small">
          <div className="text-center">
            {stats ? (
              <>
                <div className="text-2xl font-bold text-green-600">
                  {stats.activeFAQs}
                </div>
                <div className="text-sm text-gray-600">Ativas</div>
              </>
            ) : (
              <Spin size="small" />
            )}
          </div>
        </Card>
      </div>

      {/* Table */}
      <Card>
        <Table
          columns={columns}
          dataSource={faqs}
          rowKey="id"
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (total, range) =>
              `${range[0]}-${range[1]} de ${total} FAQs`,
            pageSizeOptions: ["10", "20", "50"],
            onChange: handlePageChange,
            onShowSizeChange: handlePageChange,
          }}
          scroll={{ x: 600 }}
          className="faq-table"
        />
      </Card>

      {/* Modal */}
      <FAQModal
        visible={isModalVisible}
        faq={selectedFAQ}
        onCancel={() => {
          setIsModalVisible(false);
          setSelectedFAQ(null);
        }}
        onSuccess={handleSaveFAQ}
      />
    </div>
  );
};
