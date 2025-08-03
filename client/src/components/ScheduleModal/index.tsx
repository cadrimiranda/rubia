import React, { useState } from "react";
import { Modal, DatePicker, Select, Input, Button, message } from "antd";
import { Calendar, Clock, User, Heart } from "lucide-react";
import type { Donor } from "../../types/types";
import dayjs from "dayjs";

const { Option } = Select;
const { TextArea } = Input;

interface ScheduleModalProps {
  show: boolean;
  donor: Donor | null;
  onClose: () => void;
  onSchedule: (scheduleData: ScheduleData) => void;
}

interface ScheduleData {
  date: string;
  time: string;
  type: string;
  notes: string;
}

export const ScheduleModal: React.FC<ScheduleModalProps> = ({
  show,
  donor,
  onClose,
  onSchedule,
}) => {
  const [scheduleData, setScheduleData] = useState<ScheduleData>({
    date: "",
    time: "",
    type: "doacao",
    notes: "",
  });

  const handleSchedule = () => {
    if (!scheduleData.date || !scheduleData.time) {
      message.error("Selecione data e horário!");
      return;
    }

    onSchedule(scheduleData);
    onClose();

    // Reset form
    setScheduleData({
      date: "",
      time: "",
      type: "doacao",
      notes: "",
    });
  };

  const handleCancel = () => {
    onClose();
    // Reset form
    setScheduleData({
      date: "",
      time: "",
      type: "doacao",
      notes: "",
    });
  };

  const availableTimeSlots = [
    "08:00",
    "08:30",
    "09:00",
    "09:30",
    "10:00",
    "10:30",
    "11:00",
    "11:30",
    "14:00",
    "14:30",
    "15:00",
    "15:30",
    "16:00",
    "16:30",
    "17:00",
    "17:30",
  ];

  const disabledDate = (current: dayjs.Dayjs) => {
    // Desabilitar datas passadas e domingos
    const today = dayjs().startOf("day");
    const isWeekend = current.day() === 0; // 0 = domingo
    return current < today || isWeekend;
  };

  return (
    <Modal
      title={
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-red-500 rounded-xl flex items-center justify-center">
            <Heart className="w-4 h-4 text-white" fill="currentColor" />
          </div>
          <div>
            <h3 className="text-lg font-semibold m-0">Agendar Doação</h3>
            {donor && (
              <p className="text-sm text-gray-500 m-0">
                {donor.name} • {donor.bloodType}
              </p>
            )}
          </div>
        </div>
      }
      open={show}
      onCancel={handleCancel}
      footer={null}
      width={500}
    >
      <div className="space-y-6 pt-4">
        {donor && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-center gap-3 mb-2">
              <User className="w-5 h-5 text-blue-600" />
              <h4 className="font-medium text-blue-800 m-0">
                Informações do Doador
              </h4>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <span className="text-gray-600">Tipo sanguíneo:</span>
                <span className="font-medium ml-2">{donor.bloodType}</span>
              </div>
              <div>
                <span className="text-gray-600">Última doação:</span>
                <span className="font-medium ml-2">{donor.lastDonation}</span>
              </div>
              <div>
                <span className="text-gray-600">Total de doações:</span>
                <span className="font-medium ml-2">{donor.totalDonations}</span>
              </div>
              <div>
                <span className="text-gray-600">Telefone:</span>
                <span className="font-medium ml-2">{donor.phone}</span>
              </div>
            </div>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            <Calendar className="w-4 h-4 inline mr-2" />
            Data da Doação *
          </label>
          <DatePicker
            value={scheduleData.date ? dayjs(scheduleData.date) : null}
            onChange={(date) =>
              setScheduleData((prev) => ({
                ...prev,
                date: date ? date.format("YYYY-MM-DD") : "",
              }))
            }
            disabledDate={disabledDate}
            className="w-full"
            placeholder="Selecione a data"
            format="DD/MM/YYYY"
          />
          <p className="text-xs text-gray-500 mt-1">
            Atendimento de segunda a sábado
          </p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            <Clock className="w-4 h-4 inline mr-2" />
            Horário *
          </label>
          <Select
            value={scheduleData.time}
            onChange={(time) => setScheduleData((prev) => ({ ...prev, time }))}
            className="w-full"
            placeholder="Selecione o horário"
          >
            {availableTimeSlots.map((time) => (
              <Option key={time} value={time}>
                {time}
              </Option>
            ))}
          </Select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Tipo de Agendamento
          </label>
          <Select
            value={scheduleData.type}
            onChange={(type) => setScheduleData((prev) => ({ ...prev, type }))}
            className="w-full"
          >
            <Option value="doacao">Doação de Sangue</Option>
            <Option value="triagem">Triagem Médica</Option>
            <Option value="retorno">Consulta de Retorno</Option>
            <Option value="orientacao">Orientação</Option>
          </Select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Observações
          </label>
          <TextArea
            value={scheduleData.notes}
            onChange={(e) =>
              setScheduleData((prev) => ({ ...prev, notes: e.target.value }))
            }
            rows={3}
            placeholder="Informações adicionais sobre o agendamento..."
          />
        </div>

        <div className="flex gap-3 pt-4">
          <Button
            type="primary"
            size="large"
            onClick={handleSchedule}
            disabled={!scheduleData.date || !scheduleData.time}
            className="flex-1"
          >
            Confirmar Agendamento
          </Button>
          <Button size="large" onClick={handleCancel}>
            Cancelar
          </Button>
        </div>
      </div>
    </Modal>
  );
};
