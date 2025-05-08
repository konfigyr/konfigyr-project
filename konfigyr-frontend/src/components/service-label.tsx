const DEFAULT_SERVICE_NAME = 'vault';

export type ServiceLabelProps = {
    name?: string
};

export default function ServiceLabel({ name = DEFAULT_SERVICE_NAME }: ServiceLabelProps) {
    return (
        <>
            <span className="text-secondary">konfigyr</span>.<small className="font-mono">{name}</small>
        </>
    );
}
