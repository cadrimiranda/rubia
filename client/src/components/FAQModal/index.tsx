import React, { useState, useEffect } from "react";
import {
  Modal,
  Form,
  Input,
  Button,
  message,
  Typography,
} from "antd";
import { type FAQ } from "../../services/faqService";

const { TextArea } = Input;
const { Title } = Typography;

interface FAQModalProps {
  visible: boolean;
  faq: FAQ | null;
  onCancel: () => void;
  onSuccess: (faq: Partial<FAQ>) => void;
}


export const FAQModal: React.FC<FAQModalProps> = ({
  visible,
  faq,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (visible && faq) {
      form.setFieldsValue({
        question: faq.question,
        answer: faq.answer,
        keywords: faq.keywords.join(", "),
        triggers: faq.triggers.join(", "),
      });
    } else if (visible && !faq) {
      form.resetFields();
    }
  }, [visible, faq, form]);

  const handleSubmit = async (values: any) => {
    setLoading(true);
    try {
      const faqData: Partial<FAQ> = {
        question: values.question,
        answer: values.answer,
        keywords: values.keywords
          ? values.keywords
              .split(",")
              .map((k: string) => k.trim())
              .filter(Boolean)
          : [],
        triggers: values.triggers
          ? values.triggers
              .split(",")
              .map((t: string) => t.trim())
              .filter(Boolean)
          : [],
      };

      await onSuccess(faqData);
    } catch (error) {
      message.error("Erro ao salvar FAQ");
    } finally {
      setLoading(false);
    }
  };


  return (
    <Modal
      title={
        <div className="flex items-center gap-2">
          <Title level={4} className="!mb-0">
            {faq ? "Editar FAQ" : "Nova FAQ"}
          </Title>
        </div>
      }
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={900}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        className="mt-4"
      >

        <Form.Item
          name="question"
          label="Pergunta"
          rules={[{ required: true, message: "Digite a pergunta" }]}
        >
          <TextArea
            rows={2}
            placeholder="Ex: Como faço para cancelar meu pedido?"
            size="large"
          />
        </Form.Item>

        <Form.Item
          name="answer"
          label="Resposta"
          rules={[{ required: true, message: "Digite a resposta" }]}
        >
          <TextArea
            rows={4}
            placeholder="Resposta completa que a IA pode usar como base para gerar drafts..."
            size="large"
          />
        </Form.Item>

        <Form.Item
          name="keywords"
          label="Palavras-chave (separadas por vírgula)"
          tooltip="Palavras que ajudam a identificar quando esta FAQ é relevante"
        >
          <Input
            placeholder="cancelar, pedido, cancelamento, anular"
            size="large"
          />
        </Form.Item>

        <Form.Item
          name="triggers"
          label="Gatilhos (separados por vírgula)"
          tooltip="Frases específicas que ativam esta FAQ automaticamente"
        >
          <Input
            placeholder="cancelar pedido, como cancelo, quero cancelar"
            size="large"
          />
        </Form.Item>


        {/* Actions */}
        <div className="flex justify-end gap-3">
          <Button size="large" onClick={onCancel}>
            Cancelar
          </Button>
          <Button
            type="primary"
            htmlType="submit"
            loading={loading}
            size="large"
            className="bg-red-500 hover:bg-red-600 border-red-500 hover:border-red-600"
          >
            {faq ? "Atualizar" : "Criar"} FAQ
          </Button>
        </div>
      </Form>
    </Modal>
  );
};
